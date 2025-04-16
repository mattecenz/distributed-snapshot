package polimi.ds.dsnapshot.Events.CallbackContent;

import polimi.ds.dsnapshot.Connection.Messages.Message;

public class CallbackContentWithName extends CallbackContent {
    private final String eventName;
    public CallbackContentWithName(Message callBackMessage, String eventName) {
        super(callBackMessage);
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
