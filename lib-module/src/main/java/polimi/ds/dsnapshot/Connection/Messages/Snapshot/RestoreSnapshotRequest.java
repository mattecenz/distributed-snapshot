package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Snapshot.SnapshotIdentifier;

/**
 * Message sent when the application asks if it is possible to restore the snapshot.
 * Formally this message means that a 2PC phase has started in the system to decide if the snapshot can be restored or not.
 */
public class RestoreSnapshotRequest extends Message {
    /**
     * Unique identifier of the snapshot.
     */
    private final SnapshotIdentifier snapshotIdentifier;

    /**
     * Constructor of the message.
     * @param snapshotIdentifier Identifier of the snapshot to be restored.
     */
    public RestoreSnapshotRequest(SnapshotIdentifier snapshotIdentifier) {
        super(MessageID.SNAPSHOT_RESET_REQUEST,false); //TODO: require ack?
        this.snapshotIdentifier = snapshotIdentifier;
    }

    /**
     * Constructor of the message. Only used through inheritance.
     * @param snapshotIdentifier Identifier of the snapshot to be restored.
     * @param messageID Id of the message.
     */
    protected RestoreSnapshotRequest(SnapshotIdentifier snapshotIdentifier, MessageID messageID) {
        super(messageID,false); //TODO: require ack?
        this.snapshotIdentifier = snapshotIdentifier;
    }

    /**
     * Getter of the snapshot identifier.
     * @return The snapshot identifier.
     */
    public SnapshotIdentifier getSnapshotIdentifier() {
        return snapshotIdentifier;
    }
}
