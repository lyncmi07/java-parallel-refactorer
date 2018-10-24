package parallel;

import parallel.threadallocation.ThreadAllocator;

/**
 * A superclass to algorithms that implement the parallel farm algorithm.
 * @author michaellynch
 *
 */
public abstract class FarmTask implements Runnable {

    private Thread ownedThread;
    protected volatile boolean dataInputUsed;
    private Object dataInputMonitor = new Object();

    /**
     * operation to be performed in each farm
     * @param inputValues the unshared objects for each farm
     */
    protected abstract void operation(Object[] inputValues);

    /**
     * Used to place tasks onto the ThreadAllocator in the format required for this kind of farm operation.
     * @param ta the ThreadAllocator in which the farm's tasks are to be run on.
     */
    protected abstract void allocateTasks(ThreadAllocator ta);

    /**
     * Create an array of objects that is non-shared data between each farm
     * @return unshared data of each farm
     */
    protected abstract Object[] readInputData();

    @Override
	public void run() {
        Object[] inputValues;
        synchronized(this) {
            inputValues = readInputData();
        }
        dataInputUsed = true;
        synchronized(dataInputMonitor) {
            dataInputMonitor.notifyAll();
        }
        operation(inputValues);
    }

    public Object getDataInputMonitor() {
        return dataInputMonitor;
    }

    /**
     * Checks to ensure the previously inputted data has been taken up by the latest farm started.
     * @return true if the inputed has been read by the latest farm
     */
    public boolean isDataInputUsed() {
        return dataInputUsed;
    }

    public FarmTask() {
        ownedThread = null;
    }

    public void setOwnedThread() {
        this.ownedThread = Thread.currentThread();
    }

    public Thread getOwnedThread() {
        return ownedThread;
    }
}