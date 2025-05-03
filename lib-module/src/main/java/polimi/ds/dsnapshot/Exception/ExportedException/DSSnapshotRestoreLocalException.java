package polimi.ds.dsnapshot.Exception.ExportedException;

public class DSSnapshotRestoreLocalException extends DSException {
    public DSSnapshotRestoreLocalException() {
        super("It is not possible to restore the snapshot due to a local error !");
    }
}
