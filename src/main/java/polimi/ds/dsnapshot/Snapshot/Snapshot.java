package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;

public class Snapshot {
    private final Object lock = new Object();

    private String snapshotPath = Config.SNAPSHOT_PATH;
    private final SnapshotState snapshotState;
    //private final Stack<Message> messageInputStack = new Stack<>();
    private final Consumer<CallbackContent> pushMessageReference = this::pushMessage;

    private final List<Event> inputChannels = new ArrayList<>();

    public Snapshot(List<String> eventNames, String snapshotCode, ConnectionManager connectionManager) throws EventException, IOException {
        // Get the current time as a ZonedDateTime
        ZonedDateTime now = ZonedDateTime.now();

        // Format the timestamp into a string, excluding seconds and replacing colons with underscores
        String timestampStr = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(now);

        // File name & path
        this.snapshotPath += snapshotCode + "_" + timestampStr + ".bin";

        JavaDistributedSnapshot javaDistributedSnapshot = JavaDistributedSnapshot.getInstance();
        ApplicationLayerInterface applicationLayerInterface = javaDistributedSnapshot.getApplicationLayerInterface();
        byte[] applicationState = SerializationUtils.serialize(applicationLayerInterface.getApplicationState());
        ClientSocketHandler anchorNodeHandler = connectionManager.getSpt().getAnchorNodeHandler();

        snapshotState = new SnapshotState(anchorNodeHandler.getRemoteNodeName(),connectionManager.getRoutingTable(),applicationState);
        //ThreadPool.submit(() -> saveApplicationState(anchorNode, connectionManager.getRoutingTable(), applicationState));

        if(eventNames.isEmpty()) {
            ThreadPool.submit(this::endSnapshot);
            return;
        }

        for (String eventName : eventNames) {
            Event event = EventsBroker.getEventChannel(eventName);
            inputChannels.add(event);
            event.subscribe(pushMessageReference);
        }
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

        if(!inputChannels.isEmpty()) return;

        ThreadPool.submit(this::endSnapshot);
    }

    private void endSnapshot() {
        try {
            try(FileOutputStream fos = new FileOutputStream(snapshotPath)){
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(snapshotState);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            //todo decide
        }
    }
}
