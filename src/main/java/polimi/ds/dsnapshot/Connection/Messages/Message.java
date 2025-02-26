package polimi.ds.dsnapshot.Connection.Messages;

import java.io.Serializable;

public abstract class Message implements Serializable {

    /**
     * TODO: The message has a sender, a receiver, some internal bits like ACK or else, some internal data to be serialized and sent
     * <p>
     * TODO: do I also need a identifier for the message ?
     */

    protected byte internalBits;

    public Message(boolean needAck) {

        this.internalBits = 0;

        this.internalBits = needAck ?
                (byte) (this.internalBits | MessageUtility.BIT_ACK) :
                this.internalBits;
    }

}