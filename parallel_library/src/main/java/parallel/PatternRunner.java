package parallel;

/**
 * Superclass for classes generated in users code by the refactoring tool.
 * @deprecated
 */
public abstract class PatternRunner implements Runnable {

    private int rangeStart;
    private int rangeEnd;
    private boolean rangeUsed;

    /**
     * The operation to be completed by each threads.
     * Overriden in refactoring tool's generated code.
     * 
     * @param rangeStart iteration start range
     * @param rangeEnd iteration end range
     */
    public abstract void operation(int rangeStart, int rangeEnd);

    @Override
    public void run() {
        int rangeS;
        int rangeE;
        synchronized(this) {
            rangeS = rangeStart;
            rangeE = rangeEnd;
            rangeUsed = true;
        }
        operation(rangeS, rangeE);
    }

    /**
     * Sets the range for the next thread to be started.
     * 
     * @param rangeStart iteration start range
     * @param rangeEnd iteration end range.
     */
    public void setRange(int rangeStart, int rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        rangeUsed = false;
    }

    /**
     * Check to see if the latest thread as finished recording its given range.
     * @return true is the thread has recorded its range, false if it still needs more time.
     */
    public boolean isRangeUsed() {
        return rangeUsed;
    }
}