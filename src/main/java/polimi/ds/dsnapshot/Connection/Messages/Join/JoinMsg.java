package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;

/**
 * Represents a join message used for establishing a connection in the distributed snapshot system.
 */
public class JoinMsg extends Message {

    /**
     * Constructs a JoinMsg with a specified message ID, IP address, and port number.
     *
     * @param messageID The unique identifier for this type of message.
     */
    public JoinMsg(MessageID messageID) {
        super(messageID, true);
    }

    /**
     * Constructs a JoinMsg with a default message ID for joining operations.
     *
     */
    public JoinMsg() {
        this(MessageID.MESSAGE_JOIN);
    }
}