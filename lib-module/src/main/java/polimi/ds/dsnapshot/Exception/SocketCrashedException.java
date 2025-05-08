package polimi.ds.dsnapshot.Exception;

/**
 * Exception stating that the socket has crashed, so a reaction is needed.
 */
public class SocketCrashedException extends Exception {
    /**
     * Constructor of the exception.
     */
    public SocketCrashedException() {
        super("A socket has suddenly crashed. Need to take immediate action!");
    }
}
