package polimi.ds.dsnapshot.Connection.Messages;

import java.io.Serializable;

/**
 * Generic ack sent every time a message needs it
 */
public class MessageAck implements Serializable{

    /**
     * Sequence number of the message to ack
     */
    private final int sequenceNumber;

    /**
     * Constructor of the ack
     * @param sequenceNumber sequence number of the message to be acked
     */
    public MessageAck(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Getter of the sequence number
     * @return the sequence number
     */
    public int getSequenceNumber(){return sequenceNumber;}

}
