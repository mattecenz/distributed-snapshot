package polimi.ds.dsnapshot.Events;

import polimi.ds.dsnapshot.Connection.Messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

//TODO: is necessary to sync?
public class Event {
    private final List<CallbackRunner> callbacks = new ArrayList<>();
    private final String channelName;

    public Event(String channelName) {
        this.channelName = channelName;
    }

    public void subscribe(Consumer<Message> callback) {
        callbacks.add(new CallbackRunner(callback));
    }

    public void unsubscribe(Consumer<Message> callback) {
        callbacks.removeIf(runner -> Objects.equals(runner.callback, callback));
    }

    public void publish(Message message) {
        for (CallbackRunner callback : callbacks) {
            callback.call(message);
        }
    }

    private static class CallbackRunner{
        private Consumer<Message> callback;
        public CallbackRunner(Consumer<Message> callback) {
            this.callback = callback;
        }

        public void call(Message message) {
            callback.accept(message);
        }

    }
}
