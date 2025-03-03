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
     * Unique sequence number of the message
     */
    private final int sequenceNumber;

    /**
     * Constructor with the ID
     * @param internalID id of the message (must be an unique number for each message)
     */
    public Message(MessageID internalID){
        this.internalID = internalID;
        this.sequenceNumber = MessageSQN.getNextSequenceNumber();
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

    /**
     * Getter of the internal ID
     * @return the internal id of the message
     */
    public final MessageID getInternalID() {return this.internalID;}

    /**
     * Getter of the sequence number
     * @return the sequence number of the message
     */
    public final int getSequenceNumber() {return this.sequenceNumber;}

    /**
     * Method to check if the message needs an ack by looking at his internal bits
     * @return true if the message needs an ack
     */
    public final boolean needsAck() {
        return (this.internalBits & MessageUtility.BIT_ACK) == MessageUtility.BIT_ACK;
    }

}