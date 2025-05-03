package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;

public class RestoreSnapshotResponse extends RestoreSnapshotRequest{
    private final boolean snapshotValid;

    public RestoreSnapshotResponse(RestoreSnapshotRequest restoreSnapshotRequest,boolean snapshotValid) {
        super(restoreSnapshotRequest.getSnapshotIdentifier(), MessageID.SNAPSHOT_RESET_RESPONSE);
        this.snapshotValid = snapshotValid;
    }

    public boolean isSnapshotValid() {
        return snapshotValid;
    }
}
