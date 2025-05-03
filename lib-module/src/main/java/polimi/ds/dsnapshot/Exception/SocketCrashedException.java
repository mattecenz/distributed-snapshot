package polimi.ds.dsnapshot.Exception;

public class SocketCrashedException extends Exception {
    public SocketCrashedException() {
        super("A socket has suddenly crashed. Need to take immediate action!");
    }
}
