package discovery;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;

import com.github.javaparser.ast.Node;

import codegeneration.DataVariable;

/**
 * A SharedDataDetector specialised to find shared data variables within a method declaration.
 * @author michaellynch
 *
 */
public class MethodDeclarationSharedDataDetector extends SharedDataDetector<MethodDeclaration> {

	/**
	 * Creates a new MethodDeclarationSharedDataDetector with the given method.
	 * @param method The method to search for shared variable within.
	 */
    public MethodDeclarationSharedDataDetector(MethodDeclaration method) {
        super(method);

        addTreeTraversalSpecialCase(MethodDeclaration.class, (DataVariable var, Node currentNode, NameExpr originExpr) -> {
            //Cannot have methods within methods so this must be the method in the node variable.
            return false;
        });
    }

	@Override
	protected Statement getNodeBody() {
		return node.getBody().get();
	}

}