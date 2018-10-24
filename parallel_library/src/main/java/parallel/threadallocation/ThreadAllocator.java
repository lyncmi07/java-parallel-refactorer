package parallel.threadallocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parallel.FarmTask;

/**
 * Queues tasks to be executed in parallel.
 * @author michaellynch
 *
 */
public class ThreadAllocator {

    private volatile ThreadMonitor threadMonitor;
    private volatile List<TaskThread> mainThreads;
    private volatile List<TaskThread> subThreads;
    private volatile List<FarmTask> outstandingTasks;

    private volatile Map<FarmTask, TaskGroup> allTaskGroups;

    TaskPlanter taskPlanter;


    /**
     * Creates a new ThreadAllocator and creates the given number of threads.
     * @param noOfThreads
     */
    public ThreadAllocator(int noOfThreads) {
        threadMonitor = new ThreadMonitor();
        mainThreads = new ArrayList<>();
        subThreads = new ArrayList<>();
        outstandingTasks = new ArrayList<>();
        allTaskGroups = new HashMap<>();

        for(int i = 0; i < noOfThreads; i++) {
            mainThreads.add(new TaskThread(threadMonitor, subThreads, allTaskGroups));
            mainThreads.get(i).start();
        }
        taskPlanter = new TaskPlanter(threadMonitor, mainThreads, subThreads, outstandingTasks, allTaskGroups);
        taskPlanter.start();
    }

    /**
     * Changes the number of threads being used in the ThreadAllocator.
     * It must wait for all tasks on the current threads to finish before changing the number of threads in the allocator.
     * @param noOfThreads Number of threads to be used in the ThreadAllocator.
     */
    public void setNoOfThreads(int noOfThreads) {
        waitForInactivity();

        int difference = noOfThreads - mainThreads.size();

        if(difference > 0) {
            for(int i = 0; i < difference; i++) {
                mainThreads.add(new TaskThread(threadMonitor, subThreads, allTaskGroups));
                mainThreads.get(mainThreads.size()-1).start();
            }
            return;
        }

        if(difference < 0) {
            for(int i = 0; i < -difference; i++) {
                mainThreads.get(0).shutdownThread();
                mainThreads.remove(0);
            }
        }
    }

    public int getNoOfThreads() {
        return mainThreads.size();
    }

    /**
     * Changes the number of threads being used in the ThreadAllocator.
     * The method does not wait for tasks on the current threads to finish before adding new threads.
     * The method throws an exception if the number of threads is being reduced
     * @param noOfThreads Number of threads to be used in the ThreadAllocator.
     */
    public void setNoOfThreadsImmediately(int noOfThreads) {
        int difference = noOfThreads - mainThreads.size();

        if(difference > 0) {
            for(int i = 0; i < difference; i++) {
                mainThreads.add(new TaskThread(threadMonitor, subThreads, allTaskGroups));
                mainThreads.get(mainThreads.size()-1).start();
            }
            return;
        }

        if(difference == 0) return;

        throw new RuntimeException("Cannot immediately reduce the number of threads in the ThreadAllocator, use setNoOfThreads instead.");
    }

    /**
     * Executes a new task for the given farm.
     * @param newTask The farm in which the task to execute comes from. The task is built on the current state of variables in the object.
     */
    public void execute(FarmTask newTask) {
        synchronized(allTaskGroups) {
            if(allTaskGroups.containsKey(newTask)) {
                allTaskGroups.get(newTask).addOutstandingTask();
            } else {
                allTaskGroups.put(newTask, new TaskGroup());
            }
        }
        newTask.setOwnedThread();

        synchronized(threadMonitor.getAllMonitors()) {
            outstandingTasks.add(newTask);
            //threadMonitor.notifyAllOutstandingTasks();
            threadMonitor.getAllMonitors().notifyAll();
        }

    }

    /**
     * Waits until all the tasks in a farm have completed.
     * @param taskGroupKey The farm to wait for.
     */
    public void waitUntilComplete(FarmTask taskGroupKey) {
        if(allTaskGroups.containsKey(taskGroupKey)) {
            TaskGroup tg = allTaskGroups.get(taskGroupKey);

            synchronized(tg) {
                while(!tg.completed()) {
                    try {
						tg.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
            }
        }
    }
    
    /**
     * Shuts down all threads in the ThreadAllocator without waiting for them to finish their current task.
     */
    public void shutdown() {
        for(TaskThread thread:mainThreads) {
            thread.shutdownThread();
            synchronized(thread) {
                thread.notifyAll();
            }
        }
        taskPlanter.shutdownThread();
    }

    /**
     * Waits until there are no outstanding tasks to complete and all threads have finished executing.
     */
    public void waitForInactivity() {
        while(true) {
            synchronized(threadMonitor.getAllMonitors()) {
                boolean threadsFree = true;
                for(TaskThread thread:mainThreads) {
                    if(!thread.isThreadReadyForTask()) {
                        threadsFree = false;
                        break;
                    }
                }
                
                if(threadsFree && outstandingTasks.size() == 0) {
                    break;
                }

            
                try {
                    threadMonitor.getAllMonitors().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Shuts down all threads in the ThreadAllocator once they have finished all the outstanding tasks.
     */
    public void shutdownGraceful() {
        waitForInactivity();

        shutdown();
    }
}