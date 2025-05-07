package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Exception stating that there has been a local problem trying to restore the snapshot.
 * It is an extension of the generic exported DSException.
 */
public class DSSnapshotRestoreLocalException extends DSException {
    /**
     * Constructor of the exception.
     */
    public DSSnapshotRestoreLocalException() {
        super("It is not possible to restore the snapshot due to a local error !");
    }
}
