package polimi.ds.dsnapshot.Events;

import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.util.*;

public class EventsBroker {
    private static Map<String, Event> eventChannels = new HashMap<>();
    public static Event getEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) == null) {
            throw new EventException("channel doesn't exist");
        }
        LoggerManager.getInstance().mutableInfo("get the event channel: " + channelName, Optional.of("EventBroker"), Optional.of("getEventChannel"));
        return eventChannels.get(channelName);
    }

    public static Event createEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) != null) {
            throw new EventException("channel already exist");
        }
        LoggerManager.getInstance().mutableInfo("create the event channel: " + channelName, Optional.of("EventBroker"), Optional.of("createEventChannel"));
        Event e = new Event(channelName);
        eventChannels.put(channelName, e);
        return e;
    }

    public static Event createOrGetEventChannel(String channelName, Event e) throws EventException {
        LoggerManager.getInstance().mutableInfo("create or get the event channel: " + channelName, Optional.of("EventBroker"), Optional.of("createOrGetEventChannel"));
        try{
            EventsBroker.createEventChannel(channelName);
            return e;
        } catch (EventException ex) {
            return eventChannels.get(channelName);
        }
    }

    public static void removeEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) == null) {
            throw new EventException("channel doesn't exist");
        }
        LoggerManager.getInstance().mutableInfo("remove event channel: " + channelName, Optional.of("EventBroker"), Optional.of("removeEventChannel"));

        eventChannels.remove(channelName);
    }

    public static List<String> getAllEventChannelNames() {
        LoggerManager.getInstance().mutableInfo("getAllEventChannelNames", Optional.of("EventBroker"), Optional.of("getAllEventChannelNames"));
        List<String> keys = new ArrayList<>(eventChannels.keySet());
        return keys;
    }

    public static void removeAllEventsChannel(){
        LoggerManager.getInstance().mutableInfo("remove all event channels", Optional.of("EventBroker"), Optional.of("removeAllEventsChannel"));
        eventChannels.clear();
    }
}
