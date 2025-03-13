package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class SnapshotManager {
    private static Dictionary<String, Snapshot> snapshots = new Hashtable<>();

    public synchronized static void ManageSnapshotToken(String snapshotCode, String channelIp, int channelPort) {
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

    private static void startNewSnapshot(String snapshotCode, String channelIp, int channelPort){
        List<String> eventNames = EventsBroker.getAllEventChannelNames();
        eventNames.remove(channelIp+":"+channelPort);
        try {
            Snapshot nSnapshot = new Snapshot(eventNames);
        } catch (EventException | IOException e) {
            //todo decide
        }
    }
}
