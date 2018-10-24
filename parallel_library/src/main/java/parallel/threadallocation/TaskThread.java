package parallel.threadallocation;

import java.util.List;
import java.util.Map;

import parallel.RunningTaskException;
import parallel.FarmTask;

/**
 * A thread run on the ThreadAllocator that can be assigned tasks to be run upon request.
 * @author michaellynch
 *
 */
public class TaskThread extends Thread {
    private volatile boolean threadFree;
    private volatile FarmTask runningTask;
    private volatile ThreadMonitor threadMonitor;
    private volatile  Map<FarmTask, TaskGroup> allTaskGroups;

    private volatile List<TaskThread> allSubThreads;
    private volatile TaskThread subThread;

    /**
     * Creates a new TaskThread.
     * @param threadMonitor	A monitor used to notify when the thread is ready for a new task.
     * @param subThreads A list of threads that have been created in the thread allocator to counter deadlocks that occur from embedded tasks.
     * @param allTaskGroups A map of all the farms that are currently being run on the TaskAllocator.
     */
    public TaskThread(ThreadMonitor threadMonitor, List<TaskThread> subThreads, Map<FarmTask, TaskGroup> allTaskGroups) {
        threadFree = true;
        shutdownRequired = false;
        this.threadMonitor = threadMonitor;
        this.allTaskGroups = allTaskGroups;
        runningTask = null;
        allSubThreads = subThreads;
        subThread = null;
    }


    private volatile boolean shutdownRequired;


    /**
     * Sets the sub thread to counter deadlocks.
     * @param subThread The sub thread.
     */
    public void setSubThread(TaskThread subThread) {
        this.subThread = subThread;
    }
    public TaskThread getSubThread() {
        return subThread;
    }

    @Override
    public void run() {
        while(!shutdownRequired) {
            synchronized(this) {
                while(runningTask == null) {
                    try {
                        this.wait();
                        if(shutdownRequired) return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            runningTask.run();
            
            
            synchronized(threadMonitor.getAllMonitors()) {
                //deincrement the task group
                TaskGroup tg = null;
                synchronized(allTaskGroups) {
                    tg = allTaskGroups.get(runningTask);
                }
                

                if(subThread != null) {
                    allSubThreads.remove(subThread);
                    subThread.shutdownThread();
                    subThread = null;
                }

                //System.out.println("Finished task " + runningTask);
                runningTask = null;
                threadFree = true;

                tg.removeCompletedTask();
                if(tg.completed()) {
                    synchronized(tg) {
                        tg.notifyAll();
                    }
                }
                //threadMonitor.notifyAllFreeThread();
                threadMonitor.getAllMonitors().notifyAll();
            }
        }
    }

    /**
     * Starts a new task to be run by the thread.
     * @param task The task to run.
     */
    public synchronized void setTask(FarmTask task) {
        if(threadFree) {
            runningTask = task;
            threadFree = false;
            this.notifyAll();
            

        } else {
            throw new RunningTaskException();
        }
    }

    /**
     * A check to work out if the thread is ready for a new task.
     * @return True if the thread is waiting for a new task.
     */
    public boolean isThreadReadyForTask() {
        return threadFree;
    }
    
    /**
     * Shuts down the thread.
     */
    public void shutdownThread() {
        shutdownRequired = true;
        synchronized(this) {
            this.notifyAll();
        }
    }
}