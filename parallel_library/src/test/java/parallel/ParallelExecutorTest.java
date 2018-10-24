package parallel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static java.time.Duration.ofSeconds;

public class ParallelExecutorTest {
    @Test 
    public void arrayInitialising() {
        int[] manyInts = new int[1000];
        ParallelForTest pft = new ParallelForTest(manyInts, 0, 1000, 10);

        ParallelExecutor.executeParallel(pft, 4);

        for(int i = 0; i < 1000; i++) {
            assertTrue(manyInts[i] == i, "error at i=" + i + " manyInts[i]=" + manyInts[i]);
        }
    }

    @Test
    public void unequalTasks() {
        int[] manyInts = new int[1000];
        ParallelForTest pft =  new ParallelForTest(manyInts, 0, 1000, 10);
        for(int i = 0; i < 1000; i++) {
            assertTrue(manyInts[i] == i, "error at i=" + i + " manyInts[i]=" + manyInts[i]);
        }
    }

    @Test 
    public void returningPrimitives() {
        int[] manyInts = new int[1000];
        int singleInt = 0;

        ParallelForTest pft = new ParallelForTest(manyInts, singleInt, 0, 1000, 10);

        pft = ParallelExecutor.executeParallel(pft, 4);
        singleInt = pft.singleInt;

        assertTrue(singleInt == 25);
    }


    @AfterAll
    public static void performShutdown() {
        ParallelExecutor.shutdownNow();
    }
}