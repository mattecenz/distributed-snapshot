package polimi.ds.dsnapshot.Exception.Snapshot;

/**
 * Exception stating that some error has happened when waiting for pending requests in the 2PC phase.
 */
public class SnapshotPendingRequestManagerException extends Exception {
    /**
     * Constructor of the exception.
     * @param message Message containing the information of the error.
     */
    public SnapshotPendingRequestManagerException(String message) {
        super(message);
    }
}
