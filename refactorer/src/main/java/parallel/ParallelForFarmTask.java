package parallel;

import parallel.threadallocation.ThreadAllocator;

/**
 * Skeleton code to be extended to run a for loop in parallel.
 * @author michaellynch
 *
 */
public abstract class ParallelForFarmTask extends FarmTask {
    private int rangeStart;
    private int rangeEnd;

    private int totalLoopRangeStart;
    private int totalLoopRangeEnd;
    private int noOfChunks;

    @Override
    protected void operation(Object[] inputValues) {
        operation((int)inputValues[0], (int)inputValues[1]);
    }

    /**
     * Creates a new parallel for loop with start iteration rangeStart and end iteration rangeEnd.
     * The loop is split into the given number of chunks to be executed as separate tasks in the parallel farm.
     * @param rangeStart Iteration start point.
     * @param rangeEnd Iteration end point.
     * @param noOfChunks The number of chunks to split the for loop into which will run in parallel.
     */
    public ParallelForFarmTask(int rangeStart, int rangeEnd, int noOfChunks) {
        totalLoopRangeStart = rangeStart;
        totalLoopRangeEnd = rangeEnd;
        this.noOfChunks = noOfChunks;
    }

    /**
     * Overridden with for loop operation from rangeStart to rangeEnd
     * @param rangeStart Start of loop iterations for this farm
     * @param rangeEnd End of loop iterations for this farm
     */
    protected abstract void operation(int rangeStart, int rangeEnd);

    @Override
	protected Object[] readInputData() {
        Object[] inputData = new Object[2];
        inputData[0] = rangeStart;
        inputData[1] = rangeEnd;

		return inputData;
    }
    
    /**
     * Set the start and end ranges of the next farm to be started.
     * @param rangeStart Start of loop iterations for the next farm
     * @param rangeEnd End of loop iterations for the next farm
     */
    public void setRange(int rangeStart, int rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        dataInputUsed = false;
    }

    @Override
    protected void allocateTasks(ThreadAllocator ta) {
        int total = totalLoopRangeEnd - totalLoopRangeStart;
        int range = total / noOfChunks;
        int leftover = total % noOfChunks;

        for(int i = 0; i < noOfChunks; i++) {
            if(i == noOfChunks - 1) {
                setRange(totalLoopRangeStart + i*range, totalLoopRangeStart + ((i+1)*range) + leftover);
            } else {
                setRange(totalLoopRangeStart + i*range, totalLoopRangeStart + ((i+1)*range));
            }

            ta.execute(this);

            synchronized(getDataInputMonitor()) {
                while(!isDataInputUsed()) {
                    try {
                        getDataInputMonitor().wait();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}