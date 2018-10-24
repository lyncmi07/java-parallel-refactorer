package parallel;

public class ParallelForTest extends ParallelForFarmTask {

    public int[] manyInts;
    public int singleInt;

    public ParallelForTest(int[] manyInts, int rangeStart, int rangeEnd, int noOfChunks) {
        super(rangeStart, rangeEnd, noOfChunks);
        this.manyInts = manyInts;
        this.singleInt = 0;
    }
    public ParallelForTest(int[] manyInts, int singleInt, int rangeStart, int rangeEnd, int noOfChunks) {
        super(rangeStart, rangeEnd, noOfChunks);
        this.manyInts = manyInts;
        this.singleInt = singleInt;
    }

    @Override
    public void operation(int rangeStart, int rangeEnd) {
        for(int i = rangeStart; i < rangeEnd; i++) {
            manyInts[i] = i;
            singleInt = 25;
        }
    }
}