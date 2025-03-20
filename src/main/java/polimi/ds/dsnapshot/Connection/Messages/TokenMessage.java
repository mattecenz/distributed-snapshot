package polimi.ds.dsnapshot.Connection.Messages;

public class TokenMessage extends Message {
    private final String snapshotId;
    private final String snapshotCreatorIp;
    private final int snapshotCreatorPort;
    protected TokenMessage(String snapshotId, String snapshotCreatorIp, int snapshotCreatorPort) {
        super(MessageID.SNAPSHOT_TOKEN);

        this.snapshotId = snapshotId;
        this.snapshotCreatorIp = snapshotCreatorIp;
        this.snapshotCreatorPort = snapshotCreatorPort;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getSnapshotCreatorIp() {
        return snapshotCreatorIp;
    }
    public int getSnapshotCreatorPort() {
        return snapshotCreatorPort;
    }
}
