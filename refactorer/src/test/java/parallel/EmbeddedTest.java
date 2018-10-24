package parallel;

public class EmbeddedTest extends ParallelForFarmTask {
    public EmbeddedTest(int rangeStart, int rangeEnd, int noOfChunks) {
        super(rangeStart, rangeEnd, noOfChunks);
    }

    @Override
    public void operation(int rangeStart, int rangeEnd) {
        for(int i = rangeStart; i < rangeEnd; i++) {
            ParallelExecutor.executeParallel(new EmbeddedTest2(0, 100, 10), 4);
        }
    }
}