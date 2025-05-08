package polimi.ds.dsnapshot.Exception;

/**
 * Exception stating that the socket trying to be accessed is closed, so no more actions can be performed on it.
 */
public class SocketClosedException extends Exception {
    /**
     * Constructor of the exception.
     */
    public SocketClosedException() {
        super("The socket is closed. Cannot send or receive messages anymore.");
    }
}
