package polimi.ds.dapplication.Message;

import java.io.Serializable;

/**
 * Generic message used from the application.
 * For our purposes it has the same structure as the messages of the library.
 */
public abstract class Message implements Serializable {

    /**
     * Internal id useful to decide the type at runtime.
     */
    private final MessageID messageID;

    /**
     * Constructor.
     * Since it is an abstract class this constructor will only be called by the subclasses.
     * @param messageID internal id of the message.
     */
    public Message(MessageID messageID) {
        this.messageID = messageID;
    }

    /**
     * Getter of the internal id.
     * @return the id of the message.
     */
    public MessageID getMessageID() {
        return this.messageID;
    }

}
