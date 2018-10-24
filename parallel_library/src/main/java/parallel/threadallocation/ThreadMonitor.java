package parallel.threadallocation;

/**
 * 
 * Currently using an object within this class as a monitor in other parts of the library.
 * Where this is being used it should be replaced with an Object as monitor. 
 * @deprecated
 * @author michaellynch
 *
 */
public class ThreadMonitor {

    private Object freeThreadMonitor;
    private Object outstandingTaskMonitor;

    private Object allMonitors;

    public ThreadMonitor() {
        freeThreadMonitor = new Object();
        outstandingTaskMonitor = new Object();
        allMonitors = new Object();
    }

    public void notifyAllFreeThread() {
        synchronized(allMonitors) {
            synchronized(freeThreadMonitor) {
                freeThreadMonitor.notifyAll();
            }
            
            allMonitors.notifyAll();
        }
    }

    public Object getFreeThreadMonitor() {
        return freeThreadMonitor;
    }
    public Object getOutstandingTaskMonitor() {
        return outstandingTaskMonitor;
    }
    public Object getAllMonitors() {
        return allMonitors;
    }


    public void notifyAllOutstandingTasks() {
        synchronized(allMonitors) {
            synchronized(outstandingTaskMonitor) {
                outstandingTaskMonitor.notifyAll();
            }
        
            allMonitors.notifyAll();
        }
    }

    public void notifyAllMonitors() {
        synchronized(allMonitors) {
            notifyAllFreeThread();
            notifyAllOutstandingTasks();
        }
    }

    public void waitAllMonitors() {
        synchronized(allMonitors) {
            try {
                allMonitors.wait();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void waitFreeThread() {
        synchronized(allMonitors) {
        synchronized(freeThreadMonitor) {
            try {
                freeThreadMonitor.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }}
    }

    public void waitOutstandingTasks() {
        synchronized(allMonitors) {
        synchronized(outstandingTaskMonitor) {
            try {
                outstandingTaskMonitor.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }}
    }
}