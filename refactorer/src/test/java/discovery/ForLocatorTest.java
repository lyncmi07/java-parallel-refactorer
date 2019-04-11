package discovery;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import org.junit.jupiter.api.Test;

public class ForLocatorTest {
    ForLocator forLocator;

    @Test
    public void fullFileDiscovery() throws FileNotFoundException{

        FileInputStream in = new FileInputStream("example_classes/tests/ForLocatorTestClass.java");

        CompilationUnit cu = JavaParser.parse(in);

        forLocator = new ForLocator(cu.getClassByName("ForLocatorTestClass").get());

        ForManager fm = forLocator.getForManager();

        assertNotNull(fm.getForLoopByLineAndColumn(3, 9));
        assertNotNull(fm.getForLoopByLineAndColumn(5, 13));
        assertNotNull(fm.getForLoopByLineAndColumn(10, 13));
        //for each statement add (11,17) not for loop
        assertNull(fm.getForLoopByLineAndColumn(11, 17));
        assertNotNull(fm.getForLoopByLineAndColumn(16, 13));
        assertNotNull(fm.getForLoopByLineAndColumn(25, 9));
    }
}