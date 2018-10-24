package parallel;

/**
 * Exception thrown when the task allocator attempts to assign a task to a thread that is currently still running a previous tast.
 * @author michaellynch
 *
 */
public class RunningTaskException extends RuntimeException {
    public RunningTaskException() {
        super("A task is already running on this thread. A new task cannot be assigned.");
    }
}