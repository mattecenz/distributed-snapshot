package polimi.ds.dsnapshot.Events.CallbackContent;

import polimi.ds.dsnapshot.Connection.Messages.Message;

import java.io.Serializable;

/**
 * Class which represents a generic callback function.
 */
public class CallbackContent implements Serializable {
    /**
     * Message received when the callback is invoked.
     */
    private final Message callBackMessage;

    /**
     * Constructor of the class.
     * @param callBackMessage Message received.
     */
    public CallbackContent(Message callBackMessage) {
        this.callBackMessage = callBackMessage;
    }

    /**
     * Getter of the callback message.
     * @return The callback message.
     */
    public Message getCallBackMessage() {
        return callBackMessage;
    }
}
