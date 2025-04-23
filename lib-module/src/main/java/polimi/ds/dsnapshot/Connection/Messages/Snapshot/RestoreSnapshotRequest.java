package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Snapshot.SnapshotIdentifier;

public class RestoreSnapshotRequest extends Message {
    private final SnapshotIdentifier snapshotIdentifier;

    public RestoreSnapshotRequest(SnapshotIdentifier snapshotIdentifier) {
        super(MessageID.SNAPSHOT_RESET_REQUEST,false); //TODO: require ack?
        this.snapshotIdentifier = snapshotIdentifier;
    }

    protected RestoreSnapshotRequest(SnapshotIdentifier snapshotIdentifier, MessageID messageID) {
        super(messageID,false); //TODO: require ack?
        this.snapshotIdentifier = snapshotIdentifier;
    }

    public SnapshotIdentifier getSnapshotIdentifier() {
        return snapshotIdentifier;
    }
}
