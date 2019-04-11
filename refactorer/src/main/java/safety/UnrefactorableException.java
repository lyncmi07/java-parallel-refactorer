package safety;

/**
 * Exception thrown when the user's code cannot be refactored due to problems with the structure of the code.
 * @author michaellynch
 *
 */
public class UnrefactorableException extends Exception {
	/**
	 * Creates an UnrefactorableException with the given message explaining the reason.
	 * @param message The reason for the unrefactorability of the code.
	 */
    public UnrefactorableException(String message) {
        super("The chosen for loop cannot be refactored for parallel: " + message);
    }
    
    /**
     * Creates an UnrefactorableException.
     */
    public UnrefactorableException() {
        super("The chosen for loop cannot be refactored for parallel.");
    }
}