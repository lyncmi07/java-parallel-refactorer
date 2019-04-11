package safety;

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithStatements;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import codegeneration.DataVariable;
import discovery.CodeNavigation;
import discovery.MethodDeclarationSharedDataDetector;
import discovery.SharedDataDetector;

/**
 * Performs dataflow analysis on variables to check to ensure that the data that is held within 
 * non-shared variables is safe to be edited in parallel.
 * @author michaellynch
 *
 */
public class DataflowAnalysis {

    /*private NameExpr evalExpr;

    public DataflowAnalysis(NameExpr evalExpr) {
        this.evalExpr = evalExpr;
    }*/

	/**
	 * Evaluates a NameExpr to ensure that at its position in the code the data held within it is safe to be edited
	 * @param evalExpr The NameExpr to evaluate for safety.
	 * @param sdd The SharedDataDetector of the node that the NameExpr is contained within.
	 * @return True is the given NameExpr holds data that is shared among multiple threads making it unsafe for editing.
	 * @throws CannotEvaluateException When it cannot be determined if the NameExpr holds shared data or not.
	 */
    public static boolean holdsSharedData(NameExpr evalExpr, SharedDataDetector sdd) throws CannotEvaluateException {
        if(sdd.isNameExprSharedData(evalExpr)) {
            return true;
        }

        DataVariable dv = new DataVariable(evalExpr);
        if(dv.getType().isPrimitiveType()) {
            return false;
        }

        //return detectSharedDataFromControlFlow(dv, evalExpr, null, evalExpr, sdd);
        int sharedDataVal = detectSharedDataFromControlFlow(dv, evalExpr, null, evalExpr, sdd, null, false);

        switch(sharedDataVal) {
        case 0:
            //System.out.println("throwing exception on " + evalExpr);
            throw new CannotEvaluateException();
        case 1:
            return false;
        case 2:
            return true;
        }

        throw new RuntimeException("Program should not reach this point. Contact developer.");
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, Node currentNode, Node previousNode, NameExpr originalExpr, SharedDataDetector sdd, Node maximumParentNode, boolean reachedByChild) throws CannotEvaluateException {

        if(currentNode instanceof BlockStmt) {
            int blockVal =  detectSharedDataFromControlFlow(var, (BlockStmt)currentNode, previousNode, originalExpr, sdd);
            if(blockVal != 0) {
                return blockVal;
            }
        } else if(currentNode instanceof IfStmt && !reachedByChild) {
            int ifVal = detectSharedDataFromControlFlow(var, (IfStmt)currentNode, originalExpr, sdd);
            if(ifVal != 0) {
                return ifVal;
            }
        } else if(currentNode instanceof CatchClause && paramIsVar(((CatchClause)currentNode).getParameter(), var)) {
            return 1;
        } else if(currentNode instanceof SwitchEntryStmt) {
            int switchEntryVal = detectSharedDataFromControlFlow(var, (SwitchEntryStmt)currentNode, previousNode, originalExpr, sdd);
            if(switchEntryVal != 0) {
                return switchEntryVal;
            }
        } else if(currentNode instanceof SwitchStmt && !reachedByChild) {
            int switchVal = detectSharedDataFromControlFlow(var, (SwitchStmt)currentNode, originalExpr, sdd);
            if(switchVal != 0) {
                return switchVal;
            }
        }


        if(currentNode.getParentNode().isPresent() && currentNode != maximumParentNode) {
            return detectSharedDataFromControlFlow(var, currentNode.getParentNode().get(), currentNode, originalExpr, sdd, maximumParentNode, true);
        }

        return 0;
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, NodeWithStatements currentNode, Node previousNode, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException {
        NodeList<Statement> allStmts = currentNode.getStatements();

        int finalPos = allStmts.size();

        if(previousNode != null) {
            for(int i = 0; i < allStmts.size(); i++) {
                if(allStmts.get(i) == previousNode) {
                    finalPos = i;
                    break;
                }
            }
        }

        for(int i = finalPos-1; i >= 0; i--) {
            if(allStmts.get(i).isExpressionStmt()){
                try {
                    return detectSharedDataFromControlFlow(var, allStmts.get(i).asExpressionStmt(), originalExpr, sdd);
                } catch (NoReturnException e) {}
            } else if(allStmts.get(i).isIfStmt()) {
                int ifVal = detectSharedDataFromControlFlow(var, allStmts.get(i).asIfStmt(), originalExpr, sdd);
                if(ifVal != 0) return ifVal;
            } else if(allStmts.get(i).isTryStmt()) {
                int tryVal = detectSharedDataFromControlFlow(var, allStmts.get(i).asTryStmt(), originalExpr, sdd);
                if(tryVal != 0) return tryVal;
            } else if(allStmts.get(i).isSwitchStmt()) {
                int switchVal = detectSharedDataFromControlFlow(var, allStmts.get(i).asSwitchStmt(), originalExpr, sdd);
                if(switchVal != 0) return switchVal;
            }
        }

        return 0;
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, ExpressionStmt currentNode, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException, NoReturnException {
        Expression expr = currentNode.getExpression();
        
        if(expr.isAssignExpr() && assignsToVariable(expr.asAssignExpr(), var)) {
            return detectSharedDataFromControlFlow(var, expr.asAssignExpr(), originalExpr, sdd);
        } else if(expr.isVariableDeclarationExpr()) {
            int assignsVar = assignsToVariable(expr.asVariableDeclarationExpr(), var);

            if(assignsVar != -1) {
                return detectSharedDataFromControlFlow(var, expr.asVariableDeclarationExpr(), assignsVar, originalExpr, sdd);
            }
        }

        throw new NoReturnException();
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, IfStmt currentNode, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException {
        //System.out.println("Checking an if statement:\n" + currentNode);
        //These currently work their ways out of the if statement. Should be constrained to working within the if statement to stop the program doing the same work more than once.
        int thenShared = detectSharedDataFromControlFlow(var, currentNode.getThenStmt(), null, originalExpr, sdd, currentNode.getThenStmt(), false);
        int elseShared = 0;
        if(currentNode.getElseStmt().isPresent()) {
            //System.out.println("checking else stmt");
            //System.out.println("else stmt exists:\n");
            //System.out.println(currentNode.getElseStmt().get());
            elseShared = detectSharedDataFromControlFlow(var, currentNode.getElseStmt().get(), null, originalExpr, sdd, currentNode.getElseStmt().get(), false);
        }

        //System.out.println("returning from if statements");

        if(thenShared == 2 || elseShared == 2) {
            //some paths assign shared data so target is unsafe.
            // System.out.println("at least one of the paths assigns to unsafe data");
            return 2;
        } else if(thenShared == 0 || elseShared == 0) {
            //some paths do not contain assignments so must move further up the parent statement to find potensially problematic assignments
            // System.out.println("some paths do not have assignments, then:" + thenShared + " else:" + elseShared);
            return 0;
        } else {
            //all paths have assignments and all are safe so the target is safe.
            // System.out.println("All paths are safe");
            return 1;
        }
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, TryStmt currentNode, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException {

        int tryShared = detectSharedDataFromControlFlow(var, currentNode.getTryBlock(), null, originalExpr, sdd);

        int catchShared = 1;
        for(CatchClause cat:currentNode.getCatchClauses()) {
            int catchVal = detectSharedDataFromControlFlow(var, cat.getBody(), null, originalExpr, sdd, cat, false);

            if(catchVal != 1) {
                catchShared = catchVal;
                break;
            }
        }

        if(tryShared == 2 || catchShared == 2) {
            return 2;
        } else if(tryShared == 0 || catchShared == 0) {
            return 0;
        } else {
            return 1;
        }
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, SwitchStmt currentNode, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException {
        int switchShared = 0;

        for(SwitchEntryStmt switchEntry:currentNode.getEntries()) {
            if(!switchEntry.getLabel().isPresent() && switchShared == 0) {
                switchShared = 1;
            } 


            //switchEntry.getS
            int entryVal = detectSharedDataFromControlFlow(var, switchEntry, null, originalExpr, sdd);
            if(entryVal == 2) {
                switchShared = 2;
                break;
            } else if(entryVal == 0) {
                switchShared = -1;
            }
                
        }

        if(switchShared == -1) {
            return 0;
        }

        return switchShared;
    }

    
    private static int detectSharedDataFromControlFlow(DataVariable var, AssignExpr currentNode, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException {
        if(currentNode.getValue().isObjectCreationExpr() || currentNode.getValue().isLiteralExpr()) {
            return 1;
        } else if(currentNode.getValue().isNameExpr()) {
            if(holdsSharedData(currentNode.getValue().asNameExpr(), sdd)) {
                return 2;
            } else {
                return 1;
            }
        } else if(currentNode.getValue().isMethodCallExpr()) {
            return detectSharedDataFromControlFlow(var, currentNode.getValue().asMethodCallExpr(), originalExpr, sdd);
        } else {
            throw new CannotEvaluateException();
        }
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, MethodCallExpr currentNode, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException {
        ResolvedMethodDeclaration rmd = currentNode.resolve();

        if(rmd instanceof JavaParserMethodDeclaration) {
            return detectSharedDataFromControlFlow(var, ((JavaParserMethodDeclaration)rmd).getWrappedNode());
        }

        return 0;
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, MethodDeclaration method) throws CannotEvaluateException {
        MethodDeclarationSharedDataDetector mdsdd = new MethodDeclarationSharedDataDetector(method);

        if(method.getType().isVoidType()) {
            return 0;
        } else {
            List<ReturnStmt> allReturns = method.findAll(ReturnStmt.class);

            for(ReturnStmt returnStmt:allReturns) {
                Expression returnedExpression = returnStmt.getExpression().get();



                if(returnedExpression.isNameExpr()) {
                    boolean stmtShared = holdsSharedData(returnedExpression.asNameExpr(), mdsdd);
                    if(stmtShared) {
                        return 2;
                    }
                }
            }
            
            return 1;
        }
    }

    private static int detectSharedDataFromControlFlow(DataVariable var, VariableDeclarationExpr currentNode, int variableId, NameExpr originalExpr, SharedDataDetector sdd) throws CannotEvaluateException {
        if(currentNode.getVariable(variableId).getInitializer().isPresent()) {
            Expression initialiser = currentNode.getVariable(variableId).getInitializer().get();

            if(initialiser.isObjectCreationExpr() || initialiser.isLiteralExpr()) {
                return 1;
            } else if(initialiser.isNameExpr()) {
                if(holdsSharedData(initialiser.asNameExpr(), sdd)) {
                    return 2;
                } else {
                    return 1;
                }
            } else {
                throw new CannotEvaluateException();
            }
        }

        return 1;
    }

    private static boolean assignsToVariable(AssignExpr assignExpr, DataVariable var) {
        //System.out.println("checking " + assignExpr + " against " + var);
        NameExpr baseVar = CodeNavigation.getBaseVariableOfExpression(assignExpr.getTarget());
        if(baseVar == assignExpr.getTarget() && 
            baseVar.getNameAsString().equals(var.getName())) {
            return true;
        }

        return false;
    }

    private static int assignsToVariable(VariableDeclarationExpr vdExpr, DataVariable var) {

        for(int i = 0; i < vdExpr.getVariables().size(); i++) {
            VariableDeclarator vd = vdExpr.getVariable(i);

            if(vd.getNameAsString().equals(var.getName()) &&
                vd.getType().resolve().describe().equals(var.getType().toString())) {
                return i;
            }
        }

        return -1;
    }

    private static boolean paramIsVar(Parameter p, DataVariable var) {
        return p.getNameAsString().equals(var.getName()) &&
            p.getType().resolve().describe().equals(var.getType().toString());
    }
}