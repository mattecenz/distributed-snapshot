package polimi.ds.dsnapshot.Connection.Messages;

import java.io.Serializable;

public abstract class Message implements Serializable {

    /**
     * TODO: The message has a sender, a receiver, some internal bits like ACK or else, some internal data to be serialized and sent
     * <p>
     * TODO: do I also need a identifier for the message ?
     */

    /**
     * Internal bits useful for some potential services
     */
    protected byte internalBits;

    /**
     * Id of the message
     */
    protected MessageID internalID;

    /**
     * Constructor with the ID
     * @param internalID id of the message (must be an unique number for each message)
     */
    public Message(MessageID internalID){
        this.internalID = internalID;
    }

    /**
     * Constructor with the ID and a bit for ack
     * @param internalID id of the message (must be an unique number for each message)
     * @param needAck true if this message needs to receive an ack
     */
    public Message(MessageID internalID, boolean needAck) {
        this(internalID);

        this.internalBits = needAck ?
                (byte) (this.internalBits | MessageUtility.BIT_ACK) :
                0;
    }

}