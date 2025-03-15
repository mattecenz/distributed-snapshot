package polimi.ds.dsnapshot.Events;

import polimi.ds.dsnapshot.Exception.EventException;

import java.util.*;

public class EventsBroker {
    private static Dictionary<String, Event> eventChannels = new Hashtable<>();

    public static Event getEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) == null) {
            throw new EventException("channel doesn't exist");
        }
        return eventChannels.get(channelName);
    }

    public static Event createEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) != null) {
            throw new EventException("channel already exist");
        }

        Event e = new Event(channelName);
        eventChannels.put(channelName, e);
        return e;
    }

    public static Event createOrGetEventChannel(String channelName, Event e) throws EventException {
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

        eventChannels.remove(channelName);
    }

    public static List<String> getAllEventChannelNames() {
        Enumeration<String> channelNames = eventChannels.keys();
        return new ArrayList<>(Collections.singletonList(channelNames.nextElement()));
    }

    public static void removeAllEventsChannel(){
        eventChannels = new Hashtable<>();
    }
}
