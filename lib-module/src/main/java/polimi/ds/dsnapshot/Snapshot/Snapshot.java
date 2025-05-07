package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Api.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
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

/**
 * Class which represents the whole state of the snapshot to be saved.
 */
public class Snapshot {
    /**
     * TODO: refactor and remove
     * Internal lock used for locking operations
     */
    private final Object lock = new Object();
    /**
     * String representing the path where the snapshots will be saved.
     */
    private String snapshotPath = Config.getString("snapshot.path");
    /**
     * Reference to the internal state that needs to be saved in the snapshot.
     */
    // Careful this is not final anymore, maybe refactor ?
    private SnapshotState snapshotState;
    //private final Stack<Message> messageInputStack = new Stack<>();
    /**
     * Reference to the method used for pushing messages in the snapshot state.
     * It is needed by the event system.
     */
    private final Consumer<CallbackContent> pushMessageReference = this::pushMessage;
    /**
     * List of all active input channels when performing the snapshot.
     * From these channels the incoming messages will be received and saved.
     */
    private final List<Event> inputChannels = new ArrayList<>();

    /**
     * TODO: we kinda do not need the host Port I imagine since it is already in the manager
     * Constructor of the snapshot.
     * @param eventNames List of all the events
     * @param snapshotCode Unique string code of the snapshot.
     * @param connectionManager Reference to the connection manager.
     * @param hostPort Port of this host.
     * @param applicationLayerInterface Reference to the interface used to communicate with the application.
     * @throws EventException If some generic event exception happened.
     * @throws IOException If some generic
     */
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

    /**
     * Method called when a message needs to be inserted in the snapshot.
     * @param callbackContent Named callback content which is put in the snapshot.
     *                        It represents the message received from the channel
     */
    public void pushMessage(CallbackContent callbackContent) {
        CallbackContentWithName callbackContentWithName = (CallbackContentWithName) callbackContent;
        synchronized (lock) {
            snapshotState.pushMessage(callbackContentWithName);
        }
    }

    /**
     * Method called when a new token has arrived in the incoming channel.
     * It closes that event as you need to stop listening from that.
     * @param eventName Name of the event which will be unsubscribed.
     * @throws EventException If some generic event exception happened.
     */
    public void notifyNewToken(String eventName) throws EventException {
        Event event = EventsBroker.getEventChannel(eventName);
        inputChannels.remove(event);
        event.unsubscribe(pushMessageReference);

        if(inputChannels.size() >1) return;

        ThreadPool.submit(this::endSnapshot);
    }

    /**
     * Method which is called when the all the tokens from the input channels have been received.
     * It saves the whole snapshot to the file.
     */
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
