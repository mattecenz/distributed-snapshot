package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Exception stating that due to potential crashes the network is blocked,
 * hence it is not possible to perform any action.
 * It is an extension of the generic exported DSException.
 */
public class DSConnectionUnavailableException extends DSException {
    /**
     * Constructor of the exception.
     */
    public DSConnectionUnavailableException() {
        super("The connection to other nodes is not available.");
    }
}
