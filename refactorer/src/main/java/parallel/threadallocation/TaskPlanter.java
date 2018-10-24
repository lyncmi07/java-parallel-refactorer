package parallel.threadallocation;

import java.util.List;
import java.util.Map;

import parallel.FarmTask;

/**
 * Assigns tasks to the threads in the ThreadAllocator.
 * @author michaellynch
 *
 */
class TaskPlanter extends Thread {
    private volatile boolean shutdownRequired;

    private volatile ThreadMonitor threadMonitor;
    private volatile List<TaskThread> mainThreads;
    private volatile List<TaskThread> subThreads;
    private volatile List<FarmTask> outstandingTasks;

    private volatile Map<FarmTask, TaskGroup> allTaskGroups;

    private volatile boolean pickedUpTask;

    /**
     * Creates a new TaskPlanter.
     * @param threadMonitor	The thread monitor used to check notify when threads are ready for new tasks.
     * @param mainThreads List of main threads in the allocator.
     * @param subThreads List of sub threads that have been created to prevent deadlocks.
     * @param outstandingTasks A list of tasks that are to be completed on the allocator's threads.
     * @param allTaskGroups A map of all the farms that are running on the ThreadAllocator.
     */
    public TaskPlanter(ThreadMonitor threadMonitor, List<TaskThread> mainThreads, List<TaskThread> subThreads, List<FarmTask> outstandingTasks, Map<FarmTask, TaskGroup> allTaskGroups) {
        shutdownRequired = false;
        this.threadMonitor = threadMonitor;
        this.mainThreads = mainThreads;
        this.subThreads = subThreads;
        this.outstandingTasks = outstandingTasks;
        this.allTaskGroups = allTaskGroups;
        pickedUpTask = true;
    }

    /**
     * @deprecated
     */
    public boolean hasPickedUpTask() {
        return pickedUpTask;
    }

    private TaskThread getThreadOwnerFromAllocatorThreads(Thread thread) {
        if(mainThreads.contains(thread)) {
            return (TaskThread)thread;
        }
        if(subThreads.contains(thread)) {
            return (TaskThread)thread;
        }

        return null;
    }
    private boolean isSubThreadReady(TaskThread thread) {
        TaskThread subThread = thread.getSubThread();
        if(subThread != null) {
            return thread.getSubThread().isThreadReadyForTask();
        }

        return true;
    }
    private void setTaskForSubThread(TaskThread thread, FarmTask task) {
        if(thread.getSubThread() == null) {
            TaskThread subThread = new TaskThread(threadMonitor, subThreads, allTaskGroups);
            subThreads.add(subThread);
            subThread.start();
            thread.setSubThread(subThread);
        }
        thread.getSubThread().setTask(task);
    }

    @Override
    public void run() {
        while(!shutdownRequired) {
            synchronized(threadMonitor.getAllMonitors()) {
                while(outstandingTasks.size() == 0) {
                    //threadMonitor.waitOutstandingTasks();
                    //threadMonitor.waitAllMonitors();
                    try {
						threadMonitor.getAllMonitors().wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
                    }
                    
                    if(shutdownRequired) return;
                }
            }
        
            synchronized(threadMonitor.getAllMonitors()) {
                TaskThread freeThread = null;
                for(TaskThread thread:mainThreads) {
                    if(thread.isThreadReadyForTask()) {
                        freeThread = thread;
                        break;
                    }
                }

                if(freeThread == null) {
                    for(int i = 0; i < outstandingTasks.size(); i++) {
                        TaskThread owningThread = getThreadOwnerFromAllocatorThreads(outstandingTasks.get(i).getOwnedThread());
                        if(owningThread != null && isSubThreadReady(owningThread)) {
                            setTaskForSubThread(owningThread, outstandingTasks.get(i));
                            outstandingTasks.remove(i);
                            i--;
                        }
                    }
                } else {
                    synchronized(threadMonitor.getAllMonitors()) {
                        FarmTask nextTask = outstandingTasks.remove(0);
                        freeThread.setTask(nextTask);
                    }
                }

                try {
					threadMonitor.getAllMonitors().wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
            //}}

        }
    }
    
    /**
     * Prints a list of tasks that are queued to be run on the ThreadAllocator.
     */
    public void printOutstandingTasks() {
        System.out.println("--OUTSTANDING TASKS--");
        for(int i = 0; i < outstandingTasks.size(); i++) {
            System.out.print(outstandingTasks.get(i) + ",");
        }
        System.out.println();
    }

    /**
     * Shuts down the thread that is being used to allocate tasks to threads.
     */
    public void shutdownThread() {
        shutdownRequired = true;
        //the TaskPlanter may be waiting on this monitor so notify for shutting down thread
        //threadMonitor.notifyAllMonitors();
        synchronized(threadMonitor.getAllMonitors()) {
            threadMonitor.getAllMonitors().notifyAll();
        }

    }
}