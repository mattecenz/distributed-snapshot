package polimi.ds.dsnapshot.Exception.ExportedException;

public class DSSnapshotRestoreRemoteException extends DSException {
    public DSSnapshotRestoreRemoteException() {
        super("It is not possible to restore the snapshot due to a remote error !");
    }
}
