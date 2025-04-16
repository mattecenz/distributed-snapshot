package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;

/**
 * Message dedicated to communications between the application layer of the library
 */
public class ApplicationMessage extends Message {
    /**
     * Serializable generic object with all the information needed by the application layer
     */
    private final Serializable applicationContent;
    /**
     * Sender of the message
     */
    private final NodeName sender;
    /**
     * Destination of the message
     */
    private final NodeName receiver;

    /**
     * Constructor of the application message. By default it does not need an ack
     * @param applicationContent serializable object
     * @param sender name of the sender
     * @param receiver name of the receiver
     */
    public ApplicationMessage(Serializable applicationContent,NodeName sender, NodeName receiver) {
        super(MessageID.MESSAGE_APP, false);

        this.applicationContent = applicationContent;
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * Getter of the internal information known by the application
     * @return a serializable object
     */
    public Serializable getApplicationContent() {
        return this.applicationContent;
    }

    /**
     * Getter name of sender
     * @return name of sender
     */
    public NodeName getSender() {
        return this.sender;
    }

    /**
     * Getter name of receiver
     * @return name of receiver
     */
    public NodeName getReceiver() {
        return this.receiver;
    }
}
