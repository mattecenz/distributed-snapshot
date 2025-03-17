package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.NetNode;
import polimi.ds.dsnapshot.Connection.RoutingTable;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class Snapshot {
    private final Object lock = new Object();

    private String snapshotPath = "./snapshots/"; //todo config param

    private final Stack<Message> messageInputStack = new Stack<>();
    private final Consumer<Message> pushMessageReference = this::pushMessage;

    private final List<Event> inputChannels = new ArrayList<>();

    public Snapshot(List<String> eventNames, String snapshotCode, ConnectionManager connectionManager) throws EventException, IOException {
        // Get the current time as a ZonedDateTime
        ZonedDateTime now = ZonedDateTime.now();

        // Format the timestamp into a string, excluding seconds and replacing colons with underscores
        String timestampStr = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").format(now);

        // File name & path
        this.snapshotPath += snapshotCode + "_" + timestampStr + ".bin";

        ApplicationLayerInterface applicationLayerInterface = JavaDistributedSnapshot.getApplicationLayerInterface();
        byte[] applicationState = SerializationUtils.serialize(applicationLayerInterface.getApplicationState());
        ClientSocketHandler anchorNodeAndler = connectionManager.getSpt().getAnchorNodeHandler();

        NetNode anchorNode = new NetNode(anchorNodeAndler.getRemoteIp(),anchorNodeAndler.getRemotePort());
        ThreadPool.submit(() -> saveApplicationState(anchorNode, connectionManager.getRoutingTable(), applicationState));

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
        ThreadPool.submit(this::saveMessages);
    }

    private void saveApplicationState(NetNode anchorNode, RoutingTable routingTable, byte[] applicationState) {
        try {
            SnapshotState snapshotState = new SnapshotState(anchorNode,routingTable,applicationState);
            byte[] fileContent = SerializationUtils.serialize(snapshotState);

            try(FileOutputStream fos = new FileOutputStream(snapshotPath)){
                fos.write(fileContent);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            //todo decide
        }
    }

    private void saveMessages(){
        SnapshotState oldSnapshotState = SnapshotManager.getLastSnapshot();
        if(oldSnapshotState == null){}
        //todo
    }
}
