package polimi.ds.dsnapshot.Exception;

public class DSConnectionUnavailableException extends DSException {
    public DSConnectionUnavailableException() {
        super("The connection to other nodes is not available.");
    }
}
