package polimi.ds.dsnapshot.Exception.Snapshot;

/**
 * Exception stating that there was some problem during the restore process of the snapshot.
 */
public class SnapshotRestoreException extends Exception {
    /**
     * Constructor of the exception.
     * @param message Message containing the information of the error.
     */
    public SnapshotRestoreException(String message) {
        super(message);
    }
}
