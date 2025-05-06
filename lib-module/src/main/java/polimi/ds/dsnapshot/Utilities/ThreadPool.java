package polimi.ds.dsnapshot.Utilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class which wrap the thread pool and exposes it to different classes of the library.
 */
public class ThreadPool {
    /**
     * Static java executor service which handles the thread pool.
     */
    private final static ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Method used to submit the runnable operation to the thread pool
     * @param r Runnable function.
     */
    public synchronized static void submit(Runnable r) {
        executor.submit(r);
    }

    /**
     * Method used to submit the runnable operation to the thread pool with a future return object.
     * @param r Runnable function.
     * @return The future return value of the object.
     */
    public synchronized static Future<?> submitAndReturnFuture(Runnable r) {
        return executor.submit(r);
    }
}
