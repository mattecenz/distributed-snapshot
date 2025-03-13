package polimi.ds.dsnapshot.Utilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    private static ExecutorService executor = Executors.newCachedThreadPool();

    public static void submit(Runnable r) {
        executor.submit(r);
    }
}
