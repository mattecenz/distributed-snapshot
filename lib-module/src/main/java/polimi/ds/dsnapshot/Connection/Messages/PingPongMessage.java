package polimi.ds.dsnapshot.Connection.Messages;

public class PingPongMessage extends Message {
    private final boolean isFistPing;
    public PingPongMessage(boolean isFistPing) {
        super(MessageID.MESSAGE_PINGPONG, true);
        this.isFistPing = isFistPing;
    }

    public boolean isFistPing() {
        return isFistPing;
    }
}
