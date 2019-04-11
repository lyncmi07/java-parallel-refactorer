package safety;

/**
 * An exception that is thrown when the compare expression of a for loop is not valid for parallelisation.
 * @author michaellynch
 *
 */
public class CompareExprURException extends UnrefactorableException {
    public CompareExprURException(String message, int line, int column) {
        super("Problem with for loop's compare expression. Line " + line + " Column " + column + ": " + message);
    }
}