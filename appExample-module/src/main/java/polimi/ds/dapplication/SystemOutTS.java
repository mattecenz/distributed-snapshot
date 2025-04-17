package polimi.ds.dapplication;

public class SystemOutTS {

    private final static Object systemOutLock = new Object();

    public static void println(String msg){
        synchronized(systemOutLock){
            System.out.println(msg);
        }
    }

    public static void print(String msg){
        synchronized(systemOutLock){
            System.out.print(msg);
        }
    }

}
