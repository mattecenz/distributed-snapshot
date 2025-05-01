package polimi.ds.dsnapshot.Exception;

public class SocketClosedException extends Exception {
    public SocketClosedException() {
        super("The socket is closed. Cannot send or receive messages anymore.");
    }
}
