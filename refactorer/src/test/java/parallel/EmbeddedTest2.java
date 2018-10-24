package parallel;

public class EmbeddedTest2 extends ParallelForFarmTask {
    public EmbeddedTest2(int rangeStart, int rangeEnd, int noOfChunks) {
        super(rangeStart, rangeEnd, noOfChunks);
    }

    @Override 
    public void operation(int rangeStart, int rangeEnd) {
        for(int i = 0; i < rangeEnd; i++) {
            int total = i;
        }
    }
}