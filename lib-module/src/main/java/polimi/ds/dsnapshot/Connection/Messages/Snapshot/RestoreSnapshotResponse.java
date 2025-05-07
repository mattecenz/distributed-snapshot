package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;

/**
 * Message which contains the response of a node when asked if he can do the snapshot or not.
 */
public class RestoreSnapshotResponse extends RestoreSnapshotRequest{
    /**
     * Boolean which contains the decision of the node.
     */
    private final boolean snapshotValid;

    /**
     * Constructor of the message
     * @param restoreSnapshotRequest Original snapshot request. Useful for matching the identifier of the original request.
     * @param snapshotValid Boolean which stores the information of the validity of the snapshot.
     */
    public RestoreSnapshotResponse(RestoreSnapshotRequest restoreSnapshotRequest,boolean snapshotValid) {
        super(restoreSnapshotRequest.getSnapshotIdentifier(), MessageID.SNAPSHOT_RESET_RESPONSE);
        this.snapshotValid = snapshotValid;
    }

    /**
     * Getter of the internal boolean.
     * @return True if this node thinks the snapshot is valid.
     */
    public boolean isSnapshotValid() {
        return snapshotValid;
    }
}
