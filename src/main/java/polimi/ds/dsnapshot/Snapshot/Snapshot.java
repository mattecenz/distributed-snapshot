package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Snapshot {
    private final Object lock = new Object();

    private final Consumer<Message> pushMessageReference = this::pushMessage;
    private final Stack<Message> messageInputStack = new Stack<>();
    private final List<Event> inputChannels = new ArrayList<>();

    public Snapshot(List<String> eventNames) throws EventException, IOException {

        ApplicationLayerInterface applicationLayerInterface = JavaDistributedSnapshot.getApplicationLayerInterface();
        byte[] applicationState = SerializationUtils.serialize(applicationLayerInterface.getApplicationState());
        ThreadPool.submit(() -> saveApplicationState(applicationState));

        for (String eventName : eventNames) {
            Event event = EventsBroker.getEventChannel(eventName);
            inputChannels.add(event);
            event.subscribe(pushMessageReference);
        }
    }

    public void pushMessage(Message message) {
        synchronized (lock) {
            messageInputStack.push(message);
        }
    }

    public void notifyNewToken(String eventName) throws EventException {
        Event event = EventsBroker.getEventChannel(eventName);
        inputChannels.remove(event);
        event.unsubscribe(pushMessageReference);

        if(!inputChannels.isEmpty()) return;

        //end snapshot
        ThreadPool.submit(this::saveSnapshotMessages);
    }

    private void saveApplicationState(byte[] applicationState){
        //todo save application state
    }

    private void saveSnapshotMessages(){
        while(!messageInputStack.empty()){
            synchronized (lock) {
                Message message = messageInputStack.pop();
            }
            //todo save msg in memory
        }
    }
}
