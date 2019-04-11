package safety;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import discovery.ForLocator;
import discovery.ForLoopSharedDataDetector;
import discovery.ForManager;
import discovery.SharedDataDetector;

public class SafetyTests {
    static ForLocator forLocator;
    static Map<String, List<ArrayAccessExpr>> writeOnlyArrayAccessMap;
    static Method checkSharedArrayAccess;
    static Method checkSharedDataAssignment;
    static Method checkMultiAccessArraySafety;

    static ForManager fm;

    @BeforeAll
    public static void setup() throws NoSuchMethodException, SecurityException, FileNotFoundException {
        checkSharedArrayAccess = SafetyChecks.class.getDeclaredMethod("checkSharedArrayAccess", Node.class, SharedDataDetector.class, Map.class, boolean.class, String.class);
        checkSharedDataAssignment = SafetyChecks.class.getDeclaredMethod("checkSharedDataAssignment", Node.class, SharedDataDetector.class, String.class);
        checkMultiAccessArraySafety = SafetyChecks.class.getDeclaredMethod("checkMultiAccessArraySafety", Map.class);

        checkSharedArrayAccess.setAccessible(true);
        checkSharedDataAssignment.setAccessible(true);
        checkMultiAccessArraySafety.setAccessible(true);

        JavaSymbolSolver jss = new JavaSymbolSolver(new ReflectionTypeSolver());
        JavaParser.getStaticConfiguration().setSymbolResolver(jss);

        FileInputStream in = new FileInputStream("example_classes/tests/ForLoopSafety.java");
        CompilationUnit cu = JavaParser.parse(in);

        forLocator = new ForLocator(cu.getClassByName("ForLoopSafety").get());

        fm = forLocator.getForManager();
    }

    @Test
    public void arrayAccessIteratorIndex() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(7, 9);        

        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        writeOnlyArrayAccessMap = new HashMap<>();

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        //check to ensure there are no shared array accesses.
        assertFalse(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
        
    }

    @Test 
    public void arrayAccessIteratorOffsetIndex() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(11, 9);

        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        writeOnlyArrayAccessMap = new HashMap<>();

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        assertFalse(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
    }


    @Test 
    public void arrayAccessConstantIndexFail() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(15, 9);

        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        writeOnlyArrayAccessMap = new HashMap<>();

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        //check to ensure that the shared array accesses are picked up
        assertTrue(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
    }

    @Test 
    public void arrayMultiAccessSameIndexStringEquivalent() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(20, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        writeOnlyArrayAccessMap = new HashMap<>();
        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false);
        assertFalse(invokeCheckMultiAccessArraySafety(sc, writeOnlyArrayAccessMap));
    }

    @Test 
    public void arrayMultiAccessSameIndexStringDifference() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(25, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        writeOnlyArrayAccessMap = new HashMap<>();
        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false);
        assertFalse(invokeCheckMultiAccessArraySafety(sc, writeOnlyArrayAccessMap));
    }

    @Test
    public void arrayMultiAccessDifferentIndex() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(30, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        writeOnlyArrayAccessMap = new HashMap<>();
        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false);
        assertTrue(invokeCheckMultiAccessArraySafety(sc, writeOnlyArrayAccessMap));
    }

    @Test 
    public void noArrayAccess() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(35, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        writeOnlyArrayAccessMap = new HashMap<>();

        assertFalse(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
    }

    @Test 
    public void sharedVariableWriteFail() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(35, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        assertTrue(invokeCheckSharedDataAssignment(sc, forLoop, sdd));
    }

    @Test
    public void nonSharedVariableWrite() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(39, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        assertFalse(invokeCheckSharedDataAssignment(sc, forLoop, sdd));
    }

    @Test
    public void nonSharedVariableSameNameWrite() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(45, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        assertFalse(invokeCheckSharedDataAssignment(sc, forLoop, sdd));
    }

    @Test
    public void sharedVariableReferenceWarning() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(51, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        assertTrue(invokeCheckSharedDataAssignment(sc, forLoop, sdd));
    }

    @Test 
    public void noSideEffectFunction() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(56, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        writeOnlyArrayAccessMap = new HashMap<>();

        assertFalse(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
    }

    @Test 
    public void noSideEffectEmbeddedFunction() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(60, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        writeOnlyArrayAccessMap = new HashMap<>();

        assertFalse(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
    }

    @Test 
    public void sideEffectMethod() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(64, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        writeOnlyArrayAccessMap = new HashMap<>();

        assertTrue(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
    }

    @Test 
    public void sideEffectEmbeddedMethod() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ForStmt forLoop = fm.getForLoopByLineAndColumn(68, 9);
        ForLoopSharedDataDetector sdd = new ForLoopSharedDataDetector(forLoop);

        SafetyChecks sc = new SafetyChecks(forLoop, sdd);

        writeOnlyArrayAccessMap = new HashMap<>();

        assertTrue(invokeCheckSharedArrayAccess(sc, forLoop, sdd, writeOnlyArrayAccessMap, false));
    }



    private static <N extends Node> boolean invokeCheckSharedArrayAccess(SafetyChecks sc, N node, SharedDataDetector<N> sdd, Map<String, List<ArrayAccessExpr>> writeOnlyArrayAccessMap, boolean indexSafe) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Boolean rValue = (Boolean)checkSharedArrayAccess.invoke(sc, node, sdd, writeOnlyArrayAccessMap, indexSafe, "");

        return rValue;
    }

    private static <N extends Node> boolean invokeCheckSharedDataAssignment(SafetyChecks sc, N node, SharedDataDetector<N> sdd) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Boolean rValue = (Boolean)checkSharedDataAssignment.invoke(sc, node, sdd, "");

        return rValue;
    }

    private static boolean invokeCheckMultiAccessArraySafety(SafetyChecks sc, Map<String, List<ArrayAccessExpr>> writeOnlyArrayAccessMap) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Boolean rValue = (Boolean)checkMultiAccessArraySafety.invoke(sc, writeOnlyArrayAccessMap);

        return rValue;
    }


}