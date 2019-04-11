package discovery;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;

import codegeneration.DataVariable;

/**
 * A SharedDataDetector specialised to fid shared data variables within a for loop.
 * @author michaellynch
 *
 */
public class ForLoopSharedDataDetector extends SharedDataDetector<ForStmt> {

	/**
	 * Creates a new ForLoopSharedDataDetector with the given ForStmt.
	 * @param node The for loop to search for shared variables within.
	 */
    public ForLoopSharedDataDetector(ForStmt node) {
        super(node);

        addTreeTraversalSpecialCase(ForStmt.class, (DataVariable var, Node currentNode, NameExpr originExpr) -> {
            NodeList<Expression> allInitExprs = ((ForStmt)currentNode).getInitialization();

            for(Expression expr:allInitExprs) {
                if(expr.isVariableDeclarationExpr() &&
                    compareVarDecToData(expr.asVariableDeclarationExpr(), var)) {
                        return true;
                }
            }

            if(currentNode == node) {
                //got to the top of the main loop proving that this expression is referring to an outside var
                return false;
            }

            return findVarDecInTreeTraversal(var, currentNode.getParentNode().get(), originExpr);
        });
    }

    @Override
    protected Statement getNodeBody() {
        return node.getBody();
    }

    /**
     * Get the identifier of the for loop iterator.
     * @return The iterator identifier of the for loop in string format.
     */
    public String getForInitIdentifier() {
        return node.getInitialization().get(0).asVariableDeclarationExpr().getVariable(0).getNameAsString();
    }

    

}