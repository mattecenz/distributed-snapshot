package polimi.ds.dapplication;

/**
 * Utility class which provides an object useful for writing in a thread-safe manner to stdout.
 */
public class SystemOutTS {

    /**
     * Internal lock.
     */
    private final static Object systemOutLock = new Object();

    /**
     * Thread-safe println method.
     * @param msg String to be printed.
     */
    public static void println(String msg){
        synchronized(systemOutLock){
            System.out.println(msg);
        }
    }

    /**
     * Thread-safe print method.
     * @param msg String to be printed.
     */
    public static void print(String msg){
        synchronized(systemOutLock){
            System.out.print(msg);
        }
    }

}
