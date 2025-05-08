package polimi.ds.dsnapshot.Events;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Class which wraps a generic event.
 * Routines can subscribe, unsubscribe and publish content over a certain event.
 */
//TODO: is necessary to sync?
public class Event {
    /**
     * List of callbacks subscribed to the event.
     */
    private final List<CallbackRunner> callbacks = new ArrayList<>();
    /**
     * Unique name of the event channel.
     */
    private final String channelName;

    /**
     * Constructor of the event.
     * @param channelName Name of the event.
     */
    public Event(String channelName) {
        this.channelName = channelName;
    }

    /**
     * Method to subscribe to this event.
     * @param callback Consumer function called when the event is published.
     */
    public void subscribe(Consumer<CallbackContent> callback) {
        callbacks.add(new CallbackRunner(callback));
    }

    /**
     * Method to unsubscribe to an event.
     * If the callback is not present it does not do anything.
     * @param callback Consumer function to be removed from the event.
     */
    public void unsubscribe(Consumer<CallbackContent> callback) {
        callbacks.removeIf(runner -> Objects.equals(runner.callback, callback));
    }

    /**
     * Method called when an event is triggered.
     * It invokes all the corresponding methods in the subscribed entities.
     * @param message Message received when the event is triggered.
     */
    public void publish(Message message) {
        for (CallbackRunner callback : callbacks) {
            callback.call(message, channelName);
        }
    }

    /**
     * Internal class which wraps a Callback function.
     */
    private static class CallbackRunner{
        /**
         * Callback function to be called when event is triggered.
         */
        private Consumer<CallbackContent> callback;

        /**
         * Constructor of the class.
         * @param callback Callback function.
         */
        public CallbackRunner(Consumer<CallbackContent> callback) {
            this.callback = callback;
        }

        /**
         * Method invoked when the event is triggered.
         * It will execute the function specified in the constructor.
         * @param message Message received in the event.
         * @param channelName String representing the event channel name.
         */
        public void call(Message message, String channelName) {
            CallbackContentWithName callbackContent = new CallbackContentWithName(message, channelName);
            callback.accept(callbackContent);
        }

    }
}
