package polimi.ds.dsnapshot.Connection.Messages;

/**
 * Message exchanged only in the ping pong procedure.
 */
public class PingPongMessage extends Message {
    /**
     * Boolean which represents if this is the first ping.
     * Useful to initiate the ping-pong procedure.
     */
    private final boolean isFistPing;

    /**
     * Constructor of the ping pong message.
     * @param isFistPing Boolean true if the message sent is a first ping.
     */
    public PingPongMessage(boolean isFistPing) {
        super(MessageID.MESSAGE_PINGPONG, true);
        this.isFistPing = isFistPing;
    }

    /**
     * Check if the ping pong message corresponds to the first ping.
     * @return True if the message is a first ping.
     */
    public boolean isFistPing() {
        return isFistPing;
    }
}
