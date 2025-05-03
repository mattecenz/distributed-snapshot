package polimi.ds.dsnapshot.Exception.ExportedException;

public class DSConnectionUnavailableException extends DSException {
    public DSConnectionUnavailableException() {
        super("The connection to other nodes is not available.");
    }
}
