package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.io.File;

public class SnapshotManager {
    private Dictionary<String, Snapshot> snapshots = new Hashtable<>();
    private final ConnectionManager connectionManager;

    String snapshotPath = "./snapshots/"; //todo config param

    public SnapshotManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        File directory = new File(snapshotPath);
        if (!directory.exists()){
        boolean created = directory.mkdirs();
            if(!created){
                System.err.println("Failed to create directory " + snapshotPath);
                //todo: decide
            }
        }
    }

    public synchronized void ManageSnapshotToken(String snapshotCode, String channelIp, int channelPort) {
        Snapshot snapshot = snapshots.get(snapshotCode);
        if (snapshot == null) {
            startNewSnapshot(snapshotCode, channelIp, channelPort);
            return;
        }

        //receive a token for an existing snapshot => stop listening channel
        try {
            snapshot.notifyNewToken(channelIp+":"+channelPort);
        } catch (EventException e) {
            //todo decide
        }
    }

    private void startNewSnapshot(String snapshotCode, String channelIp, int channelPort){
        List<String> eventNames = EventsBroker.getAllEventChannelNames();
        eventNames.remove(channelIp+":"+channelPort);
        try {
            Snapshot nSnapshot = new Snapshot(eventNames, snapshotCode, this.connectionManager);
        } catch (EventException | IOException e) {
            System.err.println(e.getMessage());
            //todo decide
        }
    }
}
