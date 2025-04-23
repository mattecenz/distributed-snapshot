package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;

public class RestoreSnapshotRequest extends Message {
    private final int creatorPort;
    private final String creatorIp;
    private final String snapshotId;

    public RestoreSnapshotRequest(String creatorIp , int creatorPort, String snapshotId) {
        super(MessageID.SNAPSHOT_RESET_REQUEST,false); //TODO: require ack?
        this.creatorPort = creatorPort;
        this.creatorIp = creatorIp;
        this.snapshotId = snapshotId;
    }

    protected RestoreSnapshotRequest(String creatorIp , int creatorPort, String snapshotId, MessageID messageID) {
        super(messageID,false); //TODO: require ack?
        this.creatorPort = creatorPort;
        this.creatorIp = creatorIp;
        this.snapshotId = snapshotId;
    }

    public int getCreatorPort() {
        return creatorPort;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getCreatorIp() {
        return creatorIp;
    }
}
