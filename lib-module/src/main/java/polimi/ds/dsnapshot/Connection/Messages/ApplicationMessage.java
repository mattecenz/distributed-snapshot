package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;

/**
 * Message dedicated to communications between the application layer of the library.
 */
public class ApplicationMessage extends Message {
    /**
     * Serializable generic object with all the information needed by the application layer.
     */
    private final Serializable applicationContent;
    /**
     * Sender of the message.
     */
    private final NodeName sender;
    /**
     * Destination of the message.
     */
    private final NodeName receiver;

    /**
     * Constructor of the application message. By default it does not need an ack.
     * @param applicationContent Serializable object.
     * @param sender Name of the sender.
     * @param receiver Name of the receiver.
     */
    public ApplicationMessage(Serializable applicationContent,NodeName sender, NodeName receiver) {
        super(MessageID.MESSAGE_APP, false);

        this.applicationContent = applicationContent;
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * Getter of the internal information known by the application.
     * @return A serializable object.
     */
    public Serializable getApplicationContent() {
        return this.applicationContent;
    }

    /**
     * Getter name of sender.
     * @return Name of sender.
     */
    public NodeName getSender() {
        return this.sender;
    }

    /**
     * Getter name of receiver.
     * @return Name of receiver.
     */
    public NodeName getReceiver() {
        return this.receiver;
    }
}
