package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Api.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Exception.SpanningTreeNoAnchorNodeException;
import polimi.ds.dsnapshot.Api.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class Snapshot {
    private final Object lock = new Object();

    private String snapshotPath = Config.getString("snapshot.path");
    // Careful this is not final anymore, maybe refactor ?
    private SnapshotState snapshotState;
    //private final Stack<Message> messageInputStack = new Stack<>();
    private final Consumer<CallbackContent> pushMessageReference = this::pushMessage;

    private final List<Event> inputChannels = new ArrayList<>();

    public Snapshot(List<String> eventNames, String snapshotCode, ConnectionManager connectionManager, int hostPort,ApplicationLayerInterface applicationLayerInterface) throws EventException, IOException {
        // Get the current time as a ZonedDateTime
        ZonedDateTime now = ZonedDateTime.now();

        // Format the timestamp into a string, excluding seconds and replacing colons with underscores
        String timestampStr = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(now);

        // File name & path
        this.snapshotPath += snapshotCode + "-" + hostPort + "_" + timestampStr + ".bin";
        LoggerManager.getInstance().mutableInfo("starting snapshot with name " + snapshotPath, Optional.of(this.getClass().getName()), Optional.of("Snapshot"));

        Serializable applicationState = applicationLayerInterface.getApplicationState();

        LoggerManager.getInstance().mutableInfo("get application state", Optional.of(this.getClass().getName()), Optional.of("Snapshot"));

        this.snapshotState = new SnapshotState(connectionManager.getSpt(),connectionManager.getRoutingTable(),applicationState);

        LoggerManager.getInstance().mutableInfo("snapshot state created", Optional.of(this.getClass().getName()), Optional.of("Snapshot"));

        if(eventNames.size() <= 1) {
            ThreadPool.submit(this::endSnapshot);
            LoggerManager.getInstance().mutableInfo("snapshot started Correctly & end", Optional.of(this.getClass().getName()), Optional.of("Snapshot"));
            return;
        }

        for (String eventName : eventNames) {
            LoggerManager.getInstance().mutableInfo("snapshot subscribe to: " + eventName, Optional.of(this.getClass().getName()), Optional.of("Snapshot"));
            Event event = EventsBroker.getEventChannel(eventName);
            inputChannels.add(event);
            event.subscribe(pushMessageReference);
        }
        LoggerManager.getInstance().mutableInfo("snapshot started Correctly", Optional.of(this.getClass().getName()), Optional.of("Snapshot"));
    }

    public void pushMessage(CallbackContent callbackContent) {
        CallbackContentWithName callbackContentWithName = (CallbackContentWithName) callbackContent;
        synchronized (lock) {
            snapshotState.pushMessage(callbackContentWithName);
        }
    }

    public void notifyNewToken(String eventName) throws EventException {
        Event event = EventsBroker.getEventChannel(eventName);
        inputChannels.remove(event);
        event.unsubscribe(pushMessageReference);

        if(inputChannels.size() >1) return;

        ThreadPool.submit(this::endSnapshot);
    }

    private void endSnapshot() {
        LoggerManager.getInstance().mutableInfo("ending snapshot", Optional.of(this.getClass().getName()), Optional.of("endSnapshot"));
        try {
            try(FileOutputStream fos = new FileOutputStream(snapshotPath)){
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(snapshotState);
            }
            LoggerManager.getInstance().mutableInfo("snapshot saved in main memory" + snapshotPath, Optional.of(this.getClass().getName()), Optional.of("endSnapshot"));
        } catch (Exception e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "failed to serialize snapshot file: " + snapshotPath, e);
            //todo decide
        }
    }
}
