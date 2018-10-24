package parallel.threadallocation;

/**
 * As the the thread allocator can function with multiple farms being run at the same time on the same thread allocation system,
 * tasks that form part of the same farm are put into a TaskGroup to check for their joint completion.
 * @author michaellynch
 *
 */
public class TaskGroup {

    private volatile int outstandingTasks;

    /**
     * Creates a new TaskGroup with a single outstanding task.
     */
    public TaskGroup() {
        outstandingTasks = 1;
    }

    /**
     * Increments the count of outstanding tasks in this farm.
     */
    public synchronized void addOutstandingTask() {
        outstandingTasks++;
    }

    /**
     * Deincrements the count of outstanding tasks in this farm.
     */
    public synchronized void removeCompletedTask() {
        outstandingTasks--;
        if(outstandingTasks < 0) {
            throw new RuntimeException("ERROR: outstanding tasks is negative");
        }
    }

    
    /**
     * Checks that all tasks in the farm have been completed.
     * @return True if there are no tasks left to complete in this farm.
     */
    public synchronized boolean completed() {
        //System.out.println("CHECKING: task group tasks left:" + outstandingTasks);
        if(outstandingTasks <= 0) {
            return true;
        } else {
            return false;
        }
    }
}