package polimi.ds.dsnapshot.Utilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPool {
    private final static ExecutorService executor = Executors.newCachedThreadPool();

    public synchronized static void submit(Runnable r) {
        executor.submit(r);
    }

    public synchronized static Future<?> submitAndReturnFuture(Runnable r) {
        return executor.submit(r);
    }
}
