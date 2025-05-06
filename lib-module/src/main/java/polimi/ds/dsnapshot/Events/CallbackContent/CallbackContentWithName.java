package polimi.ds.dsnapshot.Events.CallbackContent;

import polimi.ds.dsnapshot.Connection.Messages.Message;

/**
 * Class which represents a callback function with also associated a name to it.
 * It extends the class CallbackContent.
 */
public class CallbackContentWithName extends CallbackContent {
    /**
     * String containing the event name.
     */
    private final String eventName;

    /**
     * Constructor of the class.
     * @param callBackMessage Message received in the event.
     * @param eventName String associated to the event channel name.
     */
    public CallbackContentWithName(Message callBackMessage, String eventName) {
        super(callBackMessage);
        this.eventName = eventName;
    }

    /**
     * Getter of the event name.
     * @return The event name.
     */
    public String getEventName() {
        return eventName;
    }
}
