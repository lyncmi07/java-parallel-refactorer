package safety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import discovery.CodeNavigation;
import discovery.ForLoopSharedDataDetector;
import discovery.MethodDeclarationSharedDataDetector;
import discovery.SharedDataDetector;

/**
 * A collection of processes for evaluating the safety of a for loop for
 * the purposes of parallelisation.  
 * @author michaellynch
 *
 */
public class SafetyChecks {

    private ForStmt forLoop;
    private ForLoopSharedDataDetector sdd;

    private WarningManager warningManager;

    /**
     * Creates a SafetyChecks object prepared for checking the safety of the given for loop.
     * @param forLoop The for loop to check for safety.
     * @param sdd The SharedDataDetector that corresponds to the given for loop.
     */
    public SafetyChecks(ForStmt forLoop, ForLoopSharedDataDetector sdd) {
        this.forLoop = forLoop;
        this.sdd = sdd;
        warningManager = new WarningManager();
    }

    /**
     * Performs safety checks on the for loop printing warnings where potential problems are found.
     * @throws UnrefactorableException When the for loop cannot be refactored.
     */
    public void performSafetyChecks() throws UnrefactorableException{
        checkForBreakStatements();
        checkSharedDataAssignment(forLoop, sdd, ".");
        writeOnlyArrayAccessMap = new HashMap<>();
        checkSharedArrayAccess(forLoop, sdd, writeOnlyArrayAccessMap, false, ".");
        checkMultiAccessArraySafety(writeOnlyArrayAccessMap);
        checkForNonConstantCompare();

        //System.out.println("Safety checks complete.");
    }

    public void displayWarnings() {
        System.out.println("Safety Warnings");

        warningManager.printWarnings();

        System.out.println("Safety Warnings Complete");
    }

    private Map<String, List<ArrayAccessExpr>> writeOnlyArrayAccessMap;

    private <N extends Node> boolean checkSharedArrayAccess(N node, SharedDataDetector<N> sdd, Map<String, List<ArrayAccessExpr>> writeOnlyArrayAccessMap, boolean indexUnsafe, String warningPostfix) {
        boolean thereIsSharedArrayAccess = false;

        List<AssignExpr> allAssigns = node.findAll(AssignExpr.class);

        for(AssignExpr assignExpr:allAssigns) {
            ArrayAccessExpr aaExpr = CodeNavigation.getFirstOfTypeInHierarchy(ArrayAccessExpr.class, assignExpr.getTarget());

            if(aaExpr != null) {
                NameExpr baseVar = CodeNavigation.getBaseVariableOfExpression(aaExpr);
                String shareStr;
                if(sdd.isNameExprSharedData(baseVar)) {
                    shareStr = "true:";
                } else {
                    shareStr = "false:";
                }
                if(writeOnlyArrayAccessMap.containsKey(shareStr + aaExpr.getName())) {
                    writeOnlyArrayAccessMap.get(shareStr + aaExpr.getName()).add(aaExpr);
                } else {
                    List<ArrayAccessExpr> arrayAccessList = new ArrayList<>();
                    arrayAccessList.add(aaExpr);
                    writeOnlyArrayAccessMap.put(shareStr + aaExpr.getName(), arrayAccessList);
                }

                if(indexUnsafe || (!checkArrayIndexSafety(aaExpr.getIndex()) &&
                    !checkArrayAccessSafety(aaExpr))) {

                    //The index is not safe 
                    if(baseVar == null || sdd.isNameExprSharedData(baseVar)) {
                        printWarning("Multiple iterations may write to the same array element in '" + assignExpr.getTarget() + "' causing a race condition" + warningPostfix,
                                assignExpr);
                        thereIsSharedArrayAccess = true;
                    } else {
                        try {
                            if(DataflowAnalysis.holdsSharedData(baseVar, sdd)) {
                                thereIsSharedArrayAccess = true;
                                printWarning("Although the variable '" + baseVar + "' is a non-shared variable, it references an outside Object, multiple iterations may write to the same array element in '" + assignExpr.getTarget() + "' causing a race condition" + warningPostfix,
                                    assignExpr);
                            }
                        } catch(CannotEvaluateException e) {
                            thereIsSharedArrayAccess = true;
                            printWarning("Although the variable '" + baseVar + "' is a non-shared variable, if it references an outside Object, multiple iterations may write to the same array element in '" + assignExpr.getTarget() + "' causing a race condition" + warningPostfix,
                                assignExpr);
                        }
                    }
                }
            }
        }

        List<MethodCallExpr> allMethodCalls = node.findAll(MethodCallExpr.class);

        for(MethodCallExpr callExpr:allMethodCalls) {
            if(callExpr.getScope().isPresent()) {
                ArrayAccessExpr aaExpr = CodeNavigation.getFirstOfTypeInHierarchy(ArrayAccessExpr.class, callExpr.getScope().get());

                if(aaExpr != null) {
                    NameExpr baseVar = CodeNavigation.getBaseVariableOfExpression(aaExpr);

                    String aaName = getArrayAccessIdentifier(aaExpr);
                    if(writeOnlyArrayAccessMap.containsKey(aaName)) {
                        writeOnlyArrayAccessMap.get(aaName).add(aaExpr);
                    } else {
                        List<ArrayAccessExpr> arrayAccessList = new ArrayList<>();
                        arrayAccessList.add(aaExpr);
                        writeOnlyArrayAccessMap.put(aaName, arrayAccessList);
                    }


                    if(indexUnsafe || !checkArrayIndexSafety(aaExpr.getIndex()) &&
                        !checkArrayAccessSafety(aaExpr)) {
                        if((baseVar == null || sdd.isNameExprSharedData(baseVar)) && !checkMethodCallSafety(callExpr, "The method call '" + callExpr + "' may have side effects.")) {
                            printWarning("Multiple iterations may write to the same array element in '" + callExpr + "' causing a race condition if the method '" + callExpr.getNameAsString() + "' has side effects on data in its object" + warningPostfix,
                                callExpr);
                            thereIsSharedArrayAccess = true;
                        } else if(!checkMethodCallSafety(callExpr, "The method call '" + callExpr + "' may have side effects.")){
                            try {
                                if(DataflowAnalysis.holdsSharedData(baseVar, sdd)) {
                                    thereIsSharedArrayAccess = true;
                                    printWarning("Although the variable '" + baseVar + "' is a non-shared variable, it references an outside Object, multiple iterations may write to the same array element in '" + callExpr + "' causing a race condition as the method '" + callExpr.getNameAsString() + "' may have side effects on data in its object" + warningPostfix,
                                        callExpr);
                                }
                            } catch(CannotEvaluateException e) {
                                thereIsSharedArrayAccess = true;
                                printWarning("Although the variable '" + baseVar + "' is a non-shared variable, if it references an outside Object, multiple iterations may write to the same array element in '" + callExpr + "' causing a race condition as the method '" + callExpr.getNameAsString() + "' may have side effects on data in its object" + warningPostfix,
                                    callExpr);
                            }
                        }
                    } else {
                        checkArrayAccessSafety(aaExpr);
                    }
                } else {
                    if(checkMethodCallSafety(callExpr, "The method call '" + callExpr + "' may have side effects, this may cause a race condition.")) {
                        thereIsSharedArrayAccess = true;
                    }
                }
            } else {
                if(checkMethodCallSafety(callExpr, "The method call '" + callExpr + "' may have side effects, this may cause a race condition.")) {
                    thereIsSharedArrayAccess = true;
                }
                
            }
        }

        return thereIsSharedArrayAccess;
    }

    private boolean checkMethodCallSafety(MethodCallExpr expr, String sideEffectWarningMessage) {
        ResolvedMethodDeclaration rmd = expr.resolve();

        if(rmd.isStatic()) {
            printWarning("The method '" + expr + "' is static, this may cause a race condition.",
                expr);
            return true;
        } else if(rmd instanceof JavaParserMethodDeclaration) {
            if(methodHasSideEffects(((JavaParserMethodDeclaration)rmd).getWrappedNode())) {
                printWarning(sideEffectWarningMessage,
                    expr);
                return true;
            }
        } else {
            printWarning("The method '" + expr + "' cannot be analysed, it may cause a race condition.");
            return true;
        }

        return false;
    }

    private boolean methodHasSideEffects(MethodDeclaration method) {
        MethodDeclarationSharedDataDetector mdsdd = new MethodDeclarationSharedDataDetector(method);

        String fullMethodName = CodeNavigation.getParentOfTypeFromNode(ClassOrInterfaceDeclaration.class, method).getNameAsString() + "." + method.getNameAsString();

        boolean methodHasSideEffects = false;

        if(checkSharedDataAssignment(method, mdsdd, " in " + fullMethodName + "(...)")) {
            methodHasSideEffects = true;
        }

        Map<String, List<ArrayAccessExpr>> writeOnlyArrayAccessMap = new HashMap<>();
        if(checkSharedArrayAccess(method, mdsdd, writeOnlyArrayAccessMap, true, " in " + fullMethodName + "(...)")) {
            methodHasSideEffects = true;
        }

        return methodHasSideEffects;
    }

    private String getArrayAccessIdentifier(ArrayAccessExpr aaExpr) {
        NameExpr baseVar = CodeNavigation.getBaseVariableOfExpression(aaExpr);
        String shareStr;
        if(sdd.isNameExprSharedData(baseVar)) {
            shareStr = "true:";
        } else {
            shareStr = "false:";
        }

        return shareStr + aaExpr.getName();
    }

    private boolean checkMultiAccessArraySafety(Map<String, List<ArrayAccessExpr>> writeOnlyArrayAccessMap) {
        List<ArrayAccessExpr> allArrayAccesses = forLoop.findAll(ArrayAccessExpr.class);

        boolean foundMultiAccessConflict = false;

        for(ArrayAccessExpr aaExpr:allArrayAccesses) {
            String aaName = getArrayAccessIdentifier(aaExpr);

            boolean foundConflict = false;

            if(writeOnlyArrayAccessMap.containsKey(aaName)) {
                //need to check if the indexes match. If they do not there is a possible race condition
                List<ArrayAccessExpr>  writeArrayAccesses = writeOnlyArrayAccessMap.get(aaName);

                for(ArrayAccessExpr writeAaExpr:writeArrayAccesses) {
                    if(!indexEquivalent(aaExpr.getIndex(), writeAaExpr.getIndex())) {
                        printWarning("Different indexes of the array '" + aaExpr.getName() + "' are being written to and accessed which may mean other iterations overlap causing a race condition.\n         '" + 
                            aaExpr + "' on line " + aaExpr.getBegin().get().line + " and '" + writeAaExpr + "' on line " + writeAaExpr.getBegin().get().line + ".");
                        foundConflict = true;
                        foundMultiAccessConflict = true;
                        break;
                    }
                }

                //A conflict on this array has already been found. No need to give another warning for it.
                if(foundConflict) {
                    writeOnlyArrayAccessMap.remove(aaName);
                }
            }
        }

        return foundMultiAccessConflict;
    }

    private boolean indexEquivalent(Expression i1, Expression i2) {
        if(i1.toString().equals(i2.toString())) return true;

        try {
            SimplifiedEquation i1Eval = SimplifiedEquation.createSimplifiedEquation(i1, sdd.getForInitIdentifier());
            SimplifiedEquation i2Eval = SimplifiedEquation.createSimplifiedEquation(i2, sdd.getForInitIdentifier());

            return i1Eval.equals(i2Eval);
        } catch(NumberFormatException n) {
            printWarning("Cannot accurately evaulate the index expressions at line " + i1.getBegin().get().line + " or line " + i2.getBegin().get().line + ".");
            return false;
        }
    }

    private boolean checkArrayIndexSafety(Expression expr) {
        if(checkArrayIndexSafety(expr, true) == 2) {

            try {
                if(SimplifiedEquation.createSimplifiedEquation(expr, sdd.getForInitIdentifier()).getITotal() != 0) {
                    return true;
                }
            } catch(NumberFormatException e) {
                printWarning("Cannot accurately evaulate the index expressions at line " + expr.getBegin().get().line + ".");
            }
        } 

        return false;
    }

    private int checkArrayIndexSafety(Expression expr, boolean innerFunction) {
        if(expr.isNameExpr()) {
            if(expr.asNameExpr().getNameAsString().equals(sdd.getForInitIdentifier())) {
                return 2;
            } else {
                return 0;
            }
        } else if(expr.isLiteralExpr()) {
            return 1;
        } else if(expr.isCastExpr()) {
            return checkArrayIndexSafety(expr.asCastExpr().getExpression(), true);
        } else if(expr.isEnclosedExpr()) {
            return checkArrayIndexSafety(expr.asEnclosedExpr().getInner(), true);
        } else if(expr.isBinaryExpr()) {
            int leftOut = checkArrayIndexSafety(expr.asBinaryExpr().getLeft(), true);
            int rightOut = checkArrayIndexSafety(expr.asBinaryExpr().getRight(), true);
            if(leftOut == 0 || rightOut == 0) {
                return 0;
            } else if(leftOut == 2 || rightOut == 2) {
                return 2;
            } else {
                return 1;
            }
        }

        return 1;
    }

    private boolean checkArrayAccessSafety(Expression expr) {
        if(expr.isArrayAccessExpr()) {
            return checkArrayAccessSafety(expr.asArrayAccessExpr());
        } else {
            Expression higherExpr = CodeNavigation.moveUpExpressionTree(expr);
            if(higherExpr != null) {
                return checkArrayAccessSafety(higherExpr);
            }

            return false;
        } 
    }

    private boolean checkArrayAccessSafety(ArrayAccessExpr expr) {
        /*if(expr.getIndex().isNameExpr() &&
                 expr.getIndex().asNameExpr().getNameAsString().equals(sdd.getForInitIdentifier())) {*/
        if(checkArrayIndexSafety(expr.getIndex())) {
            printWarning("If multiple elements in '" + expr + "' have the same reference, there may be a race condition",
                expr);
            return true;
        } else {
            return checkArrayAccessSafety(expr.getName());
        }
    }

    private void checkForBreakStatements() throws UnrefactorableException {
        List<BreakStmt> allBreaks = forLoop.findAll(BreakStmt.class);
        /*if(allBreaks.size() > 0) {
            throw new BreakStatementURException(allBreaks.get(0).getBegin().get().line, allBreaks.get(0).getBegin().get().column);
        }*/

        for(BreakStmt breakStmt:allBreaks) {
            boolean breaksOtherStmt = CodeNavigation.parentWithClassTypesExist(new Class[]{
                SwitchStmt.class,
                ForeachStmt.class,
                WhileStmt.class
            }, breakStmt);
            ForStmt breakingFor = CodeNavigation.getParentOfTypeFromNode(ForStmt.class, breakStmt);
            if(breakingFor == forLoop && !breaksOtherStmt) {
                throw new BreakStatementURException(allBreaks.get(0).getBegin().get().line, allBreaks.get(0).getBegin().get().column);
            }
        }
    }

    private <N extends Node> boolean checkSharedDataAssignment(N searchNode, SharedDataDetector<N> sdd, String warningPostfix) {
        List<AssignExpr> allAssigns = searchNode.findAll(AssignExpr.class);

        boolean thereIsSharedData = false;

        for(AssignExpr assignExpr:allAssigns) {
            if(assignExpr.getTarget().findAll(ArrayAccessExpr.class).isEmpty()) {
                NameExpr baseVar = CodeNavigation.getBaseVariableOfExpression(assignExpr.getTarget());

                

                if(sdd.isNameExprSharedData(baseVar)) {
                    thereIsSharedData = true;
                    printWarning("Assignment to '" + assignExpr.getTarget() + "' may cause a race condition" + warningPostfix,
                        baseVar);
                } else if(!baseVar.equals(assignExpr.getTarget())) {
                    try {
						if(DataflowAnalysis.holdsSharedData(baseVar, sdd)) {
                            thereIsSharedData = true;
						    printWarning(baseVar.getNameAsString() + " references non-local data, it may cause a race condition" + warningPostfix,
						        baseVar);

						}
					} catch (CannotEvaluateException e) {
                        thereIsSharedData = true;
						printWarning("If " + baseVar.getNameAsString() + " references non-local data, it may cause a race condition" + warningPostfix,
						        baseVar);
					}
                }
            }
        }

        return thereIsSharedData;
    }

    private void checkForNonConstantCompare() throws UnrefactorableException{
        if(!forLoop.getCompare().isPresent()) {
            throw new CompareExprURException("Compare expression missing from for loop.", 
                forLoop.getBegin().get().line, 
                forLoop.getBegin().get().column);
        } 

        int compareLine = forLoop.getCompare().get().getBegin().get().line;
        int compareColumn = forLoop.getCompare().get().getBegin().get().line;

        if(!forLoop.getCompare().get().isBinaryExpr()) {
            throw new CompareExprURException("Compare expression must be a boolean expression.", 
                compareLine, 
                compareColumn);

        }
        if(!compareContainsIterator(forLoop.getCompare().get().asBinaryExpr())) {
            throw new CompareExprURException("Compare expression must contain the iterator variable on one side of the binary expression",
                compareLine,
                compareColumn);
        }

        //loop can be refactorered
        if(!iteratorComparedToConstantValue(forLoop.getCompare().get().asBinaryExpr())) {
            printWarning("Iterator variable is compared to a non-constant value. Note that the parallel for loop will run the same number of iterations regardless of changes to this value during its execution",
                compareLine,
                compareColumn);
        }

    }

    private boolean compareContainsIterator(BinaryExpr compareStatement) {
        String iteratorName = sdd.getForInitIdentifier();

        return (compareStatement.getLeft().isNameExpr() && 
                compareStatement.getLeft().asNameExpr().getNameAsString().equals(iteratorName)) ||
                (compareStatement.getRight().isNameExpr() && 
                compareStatement.getRight().asNameExpr().getNameAsString().equals(iteratorName));
    }

    private boolean iteratorComparedToConstantValue(BinaryExpr compareStatement) {
        String iteratorName = sdd.getForInitIdentifier();
        int compareLine = compareStatement.getBegin().get().line;
        int compareColumn = compareStatement.getBegin().get().column;

        Expression constExpr = null;

        if(compareStatement.getLeft().isNameExpr() && compareStatement.getLeft().asNameExpr().getNameAsString().equals(iteratorName)) {
           constExpr = compareStatement.getRight(); 
        } else {
            constExpr = compareStatement.getLeft();
        }

        if(!expressionIsConstant(constExpr)) {
            printWarning("Iterator variable is compared to a non-constant value. Note that the parallel for loop will run the same number of iterations regardless of changes to this value during its execution",
            compareLine,
            compareColumn);
        } 

        return true;
    }

    private boolean expressionIsConstant(Expression expr) {
        if(expr.isCastExpr()) {
            return expressionIsConstant(expr.asCastExpr());
        } else if(expr.isBinaryExpr()) {
            return expressionIsConstant(expr.asBinaryExpr());
        } else if(expr.isLiteralExpr()) {
            return true;
        } else {
            return false;
        }
    }
    private boolean expressionIsConstant(CastExpr expression) {
        return expressionIsConstant(expression.getExpression());
    }
    private boolean expressionIsConstant(BinaryExpr expression) {
        return expressionIsConstant(expression.getLeft()) && expressionIsConstant(expression.getRight());
    }

    

    private void printWarning(String message, int line, int column) {
        //System.out.println("WARNING Line " + line + " Column " + column + ": " + message);
        warningManager.storeWarning(message, line, column);
    }

    private void printWarning(String message, Node node) {
        /*ClassOrInterfaceDeclaration cl = CodeNavigation.getParentOfTypeFromNode(ClassOrInterfaceDeclaration.class, node);

        System.out.println("WARNING Line " + node.getBegin().get().line + 
            " Column " + node.getBegin().get().column + 
            " in class " + cl.getNameAsString() + 
            ": " + message);*/
        warningManager.storeWarning(message, node);
    }

    private void printWarning(String message) {
        //System.out.println("WARNING: " + message);
        warningManager.storeWarning(message);
    }
}