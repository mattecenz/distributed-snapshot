package polimi.ds.dsnapshot.Connection.Messages;

public class PingPongMessage extends Message {
    private final boolean isFistPing;
    private final boolean isPing;
    public PingPongMessage(boolean isFistPing, boolean isPing) {
        super(MessageID.MESSAGE_PINGPONG);
        this.isFistPing = isFistPing;
        this.isPing = isPing;
    }

    public boolean isFistPing() {
        return isFistPing;
    }

    public boolean isPing() {
        return isPing;
    }
}
