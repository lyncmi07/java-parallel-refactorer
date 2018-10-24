package parallel;


import parallel.threadallocation.ThreadAllocator;

/**
 * Executes ParallelRunners generated in the users code in parallel.
 */
public class ParallelExecutor {

    private static ThreadAllocator ta = null;
    private static Object threadAllocationLock = new Object();

    /**
     * Executes the given farm task on the number of threads given.
     * @param farmTask The farm task to execute in parallel.
     * @param noOfThreads The number of threads to run the parallel task on.
     * @return The farm task with the data that it has changed over the course of execution.
     */
    public static <N extends FarmTask> N executeParallel(N farmTask, int noOfThreads) {
        synchronized(threadAllocationLock) {
            if(ta == null) {
                ta = new ThreadAllocator(noOfThreads);
            } else if(ta.getNoOfThreads() < noOfThreads) {
                ta.setNoOfThreadsImmediately(noOfThreads);
            }
        }

        farmTask.allocateTasks(ta);

        ta.waitUntilComplete(farmTask);

        return farmTask;
    }
    
    /**
     * @deprecated
     */
    public static <N extends ParallelForFarmTask> N parallel_for(int rangeStart, int rangeEnd, N patternRunner, int noOfChunks, int noOfThreads) {
        synchronized(threadAllocationLock) {
            if(ta == null) {
                ta = new ThreadAllocator(noOfThreads);
            } else if(ta.getNoOfThreads() < noOfThreads) {
                ta.setNoOfThreadsImmediately(noOfThreads);
            }
        }

        int total = rangeEnd - rangeStart;
        int range = total / noOfChunks;
        int leftover = total % noOfChunks;

        for(int i = 0; i < noOfChunks; i++) {
            if(i == noOfChunks-1) {
                patternRunner.setRange(rangeStart + i*range, rangeStart + ((i+1)*range) + leftover);
            } else {
                patternRunner.setRange(rangeStart + i*range, rangeStart + ((i+1)*range));
            }

            ta.execute(patternRunner);

            synchronized(patternRunner.getDataInputMonitor()) {
                while(!patternRunner.isDataInputUsed()) {
                    try {
						patternRunner.getDataInputMonitor().wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
            }

        }

        ta.waitUntilComplete(patternRunner);

        return patternRunner;
    }

    /**
     * Shuts down the ParallelExecutor along with its children threads once all the tasks on the children threads have finished.
     */
    public static void shutdown() {
        ta.shutdownGraceful();
    }

    /**
     * Shuts down the ParallelExecutor along with its children threads immediately without waiting for tasks on the children threads to finish.
     */
    public static void shutdownNow() {
        ta.shutdown();
    }
}