package polimi.ds.dsnapshot.Events.CallbackContent;

import polimi.ds.dsnapshot.Connection.Messages.Message;

public class CallbackContent {
    private final Message callBackMessage;

    public CallbackContent(Message callBackMessage) {
        this.callBackMessage = callBackMessage;
    }
    public Message getCallBackMessage() {
        return callBackMessage;
    }
}
