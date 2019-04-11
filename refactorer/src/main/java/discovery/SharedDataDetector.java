package discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;

import codegeneration.DataVariable;

/**
 * Used to find variables within the given node that exist from outside the node and thus may be shared 
 * with other threads if this piece of code is run on multiple threads at the same time. 
 * @author michaellynch
 *
 * @param <N> The type of the node that is to be checked for shared variables.
 */
public abstract class SharedDataDetector <N extends Node> {

    private HashMap<Class, TreeTraversalSpecialCase> allTreeTraversalSpecialCases;

    protected N node;

    /**
     * Creates a new SharedDataDetector checking the shared nature of the variables within the given node.
     * @param node The node to check for shared variables within.
     */
    public SharedDataDetector(N node) {
        this.node = node;
        allTreeTraversalSpecialCases = new HashMap<>();

        addTreeTraversalSpecialCase(BlockStmt.class, (DataVariable var, Node currentNode, NameExpr originExpr) -> {
            NodeList<Statement> allStmts = ((BlockStmt)currentNode).getStatements();

            Statement currentStmt = allStmts.get(0);
            int counter = 0;
            while(currentStmt.getEnd().get().isBefore(originExpr.getBegin().get()) && counter < allStmts.size()) {
                currentStmt = allStmts.get(counter);

                if(currentStmt.isExpressionStmt() &&
                    currentStmt.asExpressionStmt().getExpression().isVariableDeclarationExpr() && 
                    compareVarDecToData(currentStmt.asExpressionStmt().getExpression().asVariableDeclarationExpr(), var)) {
                        return true;
                }

                counter++;
            }

            return findVarDecInTreeTraversal(var, currentNode.getParentNode().get(), originExpr);
        });

        addTreeTraversalSpecialCase(ForStmt.class, (DataVariable var, Node currentNode, NameExpr originExpr) -> {
            NodeList<Expression> allInitExprs = ((ForStmt)currentNode).getInitialization();

            for(Expression expr:allInitExprs) {
                if(expr.isVariableDeclarationExpr() &&
                    compareVarDecToData(expr.asVariableDeclarationExpr(), var)) {
                        return true;
                }
            }

            return findVarDecInTreeTraversal(var, currentNode.getParentNode().get(), originExpr);
        });

        addTreeTraversalSpecialCase(CatchClause.class, (DataVariable var, Node currentNode, NameExpr originExpr) -> {
            if(compareParamToData(((CatchClause)currentNode).getParameter(), var)) {
                return true;
            }

            return findVarDecInTreeTraversal(var, currentNode.getParentNode().get(), originExpr);
        });
    }

    /**
     * Get the body of the node. The method of accessing the body differs based on different AST structures.
     * @return The Statement that represents the body of the node.
     */
    protected abstract Statement getNodeBody();


    /**
     * Compiles a list of all the variables that are shared within the node.
     * @return A List of DataVariables representing all the shared variables.
     */
    public List<DataVariable> getAllSharedDataVariables() {
        List<DataVariable> allVariables = new ArrayList<>();
        List<NameExpr> allNames = getNodeBody().findAll(NameExpr.class);

        for(NameExpr nameExpr:allNames) {
            try {
                ClassOrInterfaceType type = new ClassOrInterfaceType(null, 
                                                nameExpr.resolve().getType().describe());
                DataVariable dv = new DataVariable(type, nameExpr.getNameAsString());
                if(fullSharedDataCheck(dv) &&
                    !allVariables.contains(dv)) {
                    allVariables.add(dv);
                }
            } catch(UnsolvedSymbolException e) {
                System.out.println("Did not recognise variable '" + nameExpr + "'. Is there missing import information?");
            }
        }  

        return allVariables;
    }

    private boolean isDefinetlySharedData(DataVariable var) {
        return false;
    }

    /**
     * Peforms a check on the given DataVariable to evaluate if in this node it would be shared data.
     * @param var The DataVariable to evaluate.
     * @return True if the given variable is shared data in this context.
     */
    public boolean fullSharedDataCheck(DataVariable var) {
        if(isDefinetlySharedData(var)) {
            return true;
        }

        List<NameExpr> allNameExprs = node.findAll(NameExpr.class);

        for(NameExpr nameExpr:allNameExprs) {
            if(nameExpr.getNameAsString().equals(var.getName()) && !findVarDecInTreeTraversal(var, nameExpr, nameExpr)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check on the given NameExpr to evaluate if it is shared data.
     * @param nameExpr The NameExpr to evaluate.
     * @return True if the given NameExpr is shared data.
     */
    public boolean isNameExprSharedData(NameExpr nameExpr) {
        // if(nameExpr.getNameAsString().equals(anObject))
        try {
            ClassOrInterfaceType type = new ClassOrInterfaceType(null, 
                                                    nameExpr.resolve().getType().describe());
            DataVariable dv = new DataVariable(type, nameExpr.getNameAsString());

            if(nameExpr.getNameAsString().equals(dv.getName()) && !findVarDecInTreeTraversal(dv, nameExpr, nameExpr)) {
                return true;
            }
        } catch(UnsolvedSymbolException e) {
            System.out.println("Did not recognise variable '" + nameExpr + "'. Is there missing import information?");
        }

        return false;
    }

    /**
     * Used to edit the functionality of the tree traversal to find a variable's declaration based on 
     * different node types.
     * @author michaellynch
     *
     */
    protected interface TreeTraversalSpecialCase {
        public boolean overrideCode(DataVariable var, Node currentNode, NameExpr originExpr);
    }
    
    /**
     * Searches for the VariableDeclaration of the given variable to evaluate if it exists within the
     * node that is being checked.
     * @param var The variable which VariableDeclaration is being searched for.
     * @param currentNode The current position in the tree.
     * @param originExpr The original expression this search is being made from.
     * @return true if the variable has been declared within the checked node.
     */
    protected final boolean findVarDecInTreeTraversal(DataVariable var, Node currentNode, NameExpr originExpr) {

        for(Class cl:allTreeTraversalSpecialCases.keySet()) {
            if(cl.isInstance(currentNode)) {
                return allTreeTraversalSpecialCases.get(cl).overrideCode(var, currentNode, originExpr);
            }
        }

        if(currentNode.getParentNode().isPresent()) {
            return findVarDecInTreeTraversal(var, currentNode.getParentNode().get(), originExpr);
        }
        
        

        return false;
    }

    /**
     * Used to edit the VariableDeclaration finder to make it conform with the given node type.
     * @param cl The type of class that this special case will perform on.
     * @param specialCase The code to be run during tree traversal upon finding this class in the AST.
     */
    protected void addTreeTraversalSpecialCase(Class cl, TreeTraversalSpecialCase specialCase) {
        allTreeTraversalSpecialCases.put(cl, specialCase);
    }

    /**
     * Compares a variable declaration against a data variable to check if they refer to the same variable.
     * @param vde The VariableDeclarationExpr to compare.
     * @param var The DataVariable to compare.
     * @return True if the VariableDeclarationExpr and DataVariable refer to the same variable.
     */
    protected boolean compareVarDecToData(VariableDeclarationExpr vde, DataVariable var) {
        for(VariableDeclarator varDec:vde.getVariables()) {
            if(varDec.getNameAsString().equals(var.getName()) &&
                varDec.getType().resolve().describe().equals(var.getType().toString())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compares a Parameter against a DataVariable to check if they refer to the same variable.
     * @param p The Parameter to compare.
     * @param var The DataVariable to compare.
     * @return True if the Parameter and DataVariable refer to the same variable.
     */
    protected boolean compareParamToData(Parameter p, DataVariable var) {
        return p.getNameAsString().equals(var.getName()) &&
            p.getType().resolve().describe().equals(var.getType().toString());
    }
}