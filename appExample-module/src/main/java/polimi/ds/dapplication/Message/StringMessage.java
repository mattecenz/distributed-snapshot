package polimi.ds.dapplication.Message;

import java.io.Serializable;

/**
 * Simple class used to send a normal string message
 */
public class StringMessage extends Message {
    /**
     * String containing the message sent
     */
    private final String message;

    /**
     * Constructor of the string message object
     * @param message string to be sent
     */
    public StringMessage(String message) {
        super(MessageID.MESSAGE_STRING);
        this.message = message;
    }

    /**
     * Getter of the message
     * @return the string containing the message
     */
    public String getMessage() {
        return this.message;
    }
}
