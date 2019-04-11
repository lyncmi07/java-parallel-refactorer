package discovery;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForManagerTest {

    static ForLocator forLocator;

    @BeforeAll
    public static void locateForLoops() throws FileNotFoundException{
        FileInputStream fis = new FileInputStream("example_classes/tests/ForLocatorTestClass.java");

        CompilationUnit cu = JavaParser.parse(fis);

        forLocator = new ForLocator(cu.getClassByName("ForLocatorTestClass").get());
    }

    @Test
    public void oneLineMultiStatement(){
        ForManager forManager = forLocator.getForManager();
        assertTrue(forManager.getForLoopsByLine(25).size() == 2);
    }

    @Test
    public void incorrectLine() {
        assertTrue(forLocator.getForManager().getForLoopsByLine(22).size() == 0);
    }

    @Test
    public void incorrectLineColumn() {
        assertNull(forLocator.getForManager().getForLoopByLineAndColumn(22,3));
    }

    
}