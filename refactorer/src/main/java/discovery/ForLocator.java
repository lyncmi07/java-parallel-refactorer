package discovery;

import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;

/**
 * Locates for loops in a class and produces a ForManager to handle them.
 */
public class ForLocator {

    private Node cl;
    private ForManager forManager;

    /**
     * Locates loops in the given class producing a ForManager.
     * @param cl The class declaration to search for for statements.
     */
    public ForLocator(ClassOrInterfaceDeclaration cl) {
        this.cl = cl;
        forManager = new ForManager();
        discoverLoops();
    }

    public ForLocator(CompilationUnit cu) {
        this.cl = cu;
        forManager = new ForManager();
        discoverLoops();
    }

    private void discoverLoops() {
        List<ForStmt> allLoops = cl.findAll(ForStmt.class);
        for(ForStmt loop:allLoops) {
            forManager.addForLoop(loop);
        }
    }

    /**
     * Get for loop manager.
     * @return the ForManager for the given class
     */
    public ForManager getForManager() {
        return forManager;
    }
}