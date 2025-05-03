package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.NodeName;

public class TokenMessage extends Message {
    private final String snapshotId;
    private final NodeName snapshotCreatorName;

    public TokenMessage(String snapshotId, NodeName snapshotCreatorName) {
        super(MessageID.SNAPSHOT_TOKEN);

        this.snapshotId = snapshotId;
        this.snapshotCreatorName = snapshotCreatorName;
    }

    public String getSnapshotId() {
        return this.snapshotId;
    }

    public NodeName getSnapshotCreatorName() {
        return this.snapshotCreatorName;
    }
}
