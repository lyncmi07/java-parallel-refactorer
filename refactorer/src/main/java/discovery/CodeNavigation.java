package discovery;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;

/**
 * A helper class for navigating the user's source code AST.
 * @author michaellynch
 *
 */
public class CodeNavigation {
	
	/**
	 * Used to move up the tree of access expression statements.
	 * @param expr The expression to move up from
	 * @return Example expression a.b {@literal ->} a.
	 */
    public static Expression moveUpExpressionTree(Expression expr) {
        if(expr.isArrayAccessExpr()) {
            return expr.asArrayAccessExpr().getName();
        } else if(expr.isCastExpr()) {
            return expr.asCastExpr().getExpression();
        } else if(expr.isEnclosedExpr()) {
            return expr.asEnclosedExpr().getInner();
        } else if(expr.isFieldAccessExpr()) {
            return expr.asFieldAccessExpr().getScope();
        } else if(expr.isMethodCallExpr() && expr.asMethodCallExpr().getScope().isPresent()) {
            return expr.asMethodCallExpr().getScope().get();
        } else if(expr.isSuperExpr() && expr.asSuperExpr().getClassExpr().isPresent()) {
            return expr.asSuperExpr().getClassExpr().get();
        } else if(expr.isThisExpr() && expr.asThisExpr().getClassExpr().isPresent()) {
            return expr.asThisExpr().getClassExpr().get();
        }

        return null;
    } 

    /**
     * Moves up an access expression tree until the given class is found and is then returned.
     * @param cl The type of the class to be found.
     * @param expr The access expression to search.
     * @return The first found example of the given class in the expression. If the class does not exist in the expression 
     * then null is returned.
     */
    public static <N extends Expression> N getFirstOfTypeInHierarchy(Class<N> cl, Expression expr) {
        if(expr == null) {
            return null;
        }
        if(cl.isInstance(expr)) {
            return (N)expr;
        } else {
            return getFirstOfTypeInHierarchy(cl, moveUpExpressionTree(expr));
        }
    }

    /**
     * Moves up the tree of an access expression to find the base variable.
     * @param expr The access expression to get the base variable of.
     * @return The base variable of the given expression.
     */
    public static NameExpr getBaseVariableOfExpression(Expression expr) {
        if(expr.isNameExpr()) {
            return expr.asNameExpr();
        } else {
            Expression higherExpr = CodeNavigation.moveUpExpressionTree(expr);
            if(higherExpr != null) {
                return getBaseVariableOfExpression(higherExpr);
            }

            return null;
        }
    }

    /**
     * Moves up the AST until the given class is found and is then returned.
     * @param parentType The type of the class to be found.
     * @param node The place to start the search of the AST from.
     * @return the first found example of the given class in the AST; If the class does not exist in the AST 
     * then null is returned.
     */
    public static <N extends Node> N getParentOfTypeFromNode(Class<N> parentType, Node node) {
        if(parentType.isInstance(node)) {
            return (N)node;
        } else if(node.getParentNode().isPresent()) {
            return getParentOfTypeFromNode(parentType, node.getParentNode().get());
        } else {
            return null;
        }
    }

    /**
     * Returns true if the given list of classes exists up the tree of the given node.
     * @param classes The classes to search for
     * @param node The place to start the search of the AST from.
     * @return True if the given list of classes exists up the tree of the given node. 
     */
    public static boolean parentWithClassTypesExist(Class[] classes, Node node) {
        for(Class cl:classes) {
            if(cl.isInstance(node)) {
                return true;
            }
        }

        if(node.getParentNode().isPresent()) {
            return parentWithClassTypesExist(classes, node.getParentNode().get());
        } else {
            return false;
        }
    }
}