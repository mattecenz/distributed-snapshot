package polimi.ds.dsnapshot.Connection.Messages;

public class PingPongMessage extends Message {
    private boolean isFistPing = false;
    public PingPongMessage(boolean isFistPing) {
        super(MessageID.MESSAGE_PINGPONG);
        this.isFistPing = isFistPing;
    }

    public boolean isFistPing() {
        return isFistPing;
    }
}
