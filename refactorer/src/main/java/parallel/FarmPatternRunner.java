package parallel;

/**
 * Superclass of algorithms using the farm parallel pattern
 * 
 * @deprecated
 */
public abstract class FarmPatternRunner implements Runnable {

    protected boolean dataInputUsed;

    /**
     * operation to be performed in each farm
     * @param inputValues the unshared objects for each farm
     */
    protected abstract void operation(Object[] inputValues);

	@Override
	public void run() {
        Object[] inputValues;
        synchronized(this) {
            inputValues = readInputData();
        }
        dataInputUsed = true;
        operation(inputValues);
    }

    /**
     * Create an array of objects that is non-shared data between each farm
     * @return unshared data of each farm
     */
    protected abstract Object[] readInputData();

    /**
     * Checks to ensure the previously inputted data has been taken up by the latest farm started.
     * @return true if the inputed has been read by the latest farm
     */
    public boolean isDataInputUsed() {
        return dataInputUsed;
    }
}