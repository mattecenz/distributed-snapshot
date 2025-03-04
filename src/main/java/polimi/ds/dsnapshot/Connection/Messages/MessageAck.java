package polimi.ds.dsnapshot.Connection.Messages;

import java.io.Serializable;

/**
 * Generic ack sent every time a message needs it
 */
public class MessageAck extends Message{

    /**
     * Constructor of the ack
     * @param sequenceNumber sequence number of the message to be acked
     */
    public MessageAck(int sequenceNumber){
        super(MessageID.MESSAGE_ACK, sequenceNumber);
    }

}
