package parallel;

/**
 * Used for producing parallel for loops
 * 
 * @deprecated
 */
public abstract class ParallelForFarmPatternRunner extends FarmPatternRunner{

    private int rangeStart;
    private int rangeEnd;

	@Override
	protected void operation(Object[] inputValues) {
		operation((int)inputValues[0], (int)inputValues[1]);
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

}