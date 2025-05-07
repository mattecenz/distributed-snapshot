package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message which represents the token sent along all the channels.
 * In the distributed snapshot, when this token is received it means that the channel has to stop listening and saving
 * incoming messages.
 */
public class TokenMessage extends Message {
    /**
     * Unique identifier of the snapshot.
     */
    private final String snapshotId;
    /**
     * Name of the initiator of the snapshot.
     */
    private final NodeName snapshotCreatorName;

    /**
     * Constructor of the token message.
     * @param snapshotId Identifier of the snapshot.
     * @param snapshotCreatorName Name of the creator of the snapshot.
     */
    public TokenMessage(String snapshotId, NodeName snapshotCreatorName) {
        super(MessageID.SNAPSHOT_TOKEN);

        this.snapshotId = snapshotId;
        this.snapshotCreatorName = snapshotCreatorName;
    }

    /**
     * Getter of the identifier of the snapshot
     * @return String containing the identifier of the snapshot.
     */
    public String getSnapshotId() {
        return this.snapshotId;
    }

    /**
     * Getter of the name of the creator of the snapshot
     * @return Name of the creator of the snapshot.
     */
    public NodeName getSnapshotCreatorName() {
        return this.snapshotCreatorName;
    }
}
