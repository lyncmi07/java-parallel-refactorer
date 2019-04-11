package safety;

/**
 * Thrown when a break statement is used to break out of the loop that is to be parallelised.
 * @author michaellynch
 *
 */
public class BreakStatementURException extends UnrefactorableException {
    public BreakStatementURException(int lineNumber, int columnNumber) {
        super("break statements cannot be used in parallel for loop. Line " + lineNumber + ", Column " + columnNumber);
    }
}