package polimi.ds.dsnapshot.Events;

import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.util.*;

/**
 * Class which manages the creation and dispatching of events along the different created channels.
 * It is a class with only static objects and methods.
 * TODO: shouldnt it be better that each access to the internal map is protected ny a lock ?
 */
public class EventsBroker {
    /**
     * Internal map which associates to a name the corresponding event channel.
     */
    private static Map<String, Event> eventChannels = new HashMap<>();

    /**
     * Methot to retrieve the event channel given a name.
     * @param channelName String name of the event channel to retrieve.
     * @return The event channel associated to the name in input.
     * @throws EventException Generic event exception. It means that the channel does not exist.
     */
    public static Event getEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) == null) {
            throw new EventException("channel doesn't exist");
        }
        LoggerManager.getInstance().mutableInfo("get the event channel: " + channelName, Optional.of("EventBroker"), Optional.of("getEventChannel"));
        return eventChannels.get(channelName);
    }

    /**
     * Method used for creating a new event channel.
     * @param channelName Name of the channel to be created.
     * @return Reference to the created event.
     * @throws EventException Generic event exception. It means that an event with this name associated already exists.
     */
    public static Event createEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) != null) {
            throw new EventException("channel already exist");
        }
        LoggerManager.getInstance().mutableInfo("create the event channel: " + channelName, Optional.of("EventBroker"), Optional.of("createEventChannel"));
        Event e = new Event(channelName);
        eventChannels.put(channelName, e);
        return e;
    }

    /**
     * TODO: fix this method, is it even useful?
     * Method to create a new channel, or get the already existing one associated to the input name.
     * @param channelName Name of the channel to be created or retrieved.
     * @param e TODO: this parameter is not needed
     * @return the event (old or new) associated to the name in input.
     * @throws EventException TODO: It does not throw the exception.
     */
    public static Event createOrGetEventChannel(String channelName, Event e) throws EventException {
        LoggerManager.getInstance().mutableInfo("create or get the event channel: " + channelName, Optional.of("EventBroker"), Optional.of("createOrGetEventChannel"));
        try{
            EventsBroker.createEventChannel(channelName);
            return e;
        } catch (EventException ex) {
            return eventChannels.get(channelName);
        }
    }

    /**
     * Method to explicitly remove an event associated to the name in input.
     * @param channelName name of the event to be removed.
     * @throws EventException Generic event exceptions. It means that the channel name already does not exist.
     */
    public static void removeEventChannel(String channelName) throws EventException {
        if(eventChannels.get(channelName) == null) {
            throw new EventException("channel doesn't exist");
        }
        LoggerManager.getInstance().mutableInfo("remove event channel: " + channelName, Optional.of("EventBroker"), Optional.of("removeEventChannel"));

        eventChannels.remove(channelName);
    }

    /**
     * Return a new object containing all the active event names.
     * @return A list of names of all the active events.
     */
    public static List<String> getAllEventChannelNames() {
        LoggerManager.getInstance().mutableInfo("getAllEventChannelNames", Optional.of("EventBroker"), Optional.of("getAllEventChannelNames"));
        List<String> keys = new ArrayList<>(eventChannels.keySet());
        return keys;
    }

    /**
     * Method to manuallu clear all the event channels saved internally.
     */
    public static void removeAllEventsChannel(){
        LoggerManager.getInstance().mutableInfo("remove all event channels", Optional.of("EventBroker"), Optional.of("removeAllEventsChannel"));
        eventChannels.clear();
    }
}
