package polimi.ds.dsnapshot.Events.CallbackContent;

import polimi.ds.dsnapshot.Connection.Messages.Message;

import java.io.Serializable;

public class CallbackContent implements Serializable {
    private final Message callBackMessage;

    public CallbackContent(Message callBackMessage) {
        this.callBackMessage = callBackMessage;
    }
    public Message getCallBackMessage() {
        return callBackMessage;
    }
}
