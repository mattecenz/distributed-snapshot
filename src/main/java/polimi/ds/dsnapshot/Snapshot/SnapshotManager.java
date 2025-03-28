package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.io.File;
import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class SnapshotManager {
    private Dictionary<String, Snapshot> snapshots = new Hashtable<>();
    //TODO: remove snapshot when ends
    private final ConnectionManager connectionManager;

    private static final String snapshotPath = Config.getString("snapshot.path");

    public SnapshotManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        File directory = new File(snapshotPath);
        if (!directory.exists()){
        boolean created = directory.mkdirs();
            if(!created){
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "Failed to create directory " + snapshotPath);
                //todo: decide
            }else{
                LoggerManager.getInstance().mutableInfo("created directory" + snapshotPath, Optional.of(this.getClass().getName()), Optional.of("SnapshotManager"));
            }
        }
    }

    public synchronized boolean manageSnapshotToken(String snapshotCode, NodeName channelName) {
        Snapshot snapshot = snapshots.get(snapshotCode);
        if (snapshot == null) {
            startNewSnapshot(snapshotCode, channelName);
            return true;
        }
        LoggerManager.getInstance().mutableInfo("received token for already existing snapshot: " + snapshotCode + " from " + channelName.getIP() + ":" + channelName.getPort(), Optional.of(this.getClass().getName()), Optional.of("manageSnapshotToken"));

        //receive a token for an existing snapshot => stop listening channel
        try {
            snapshot.notifyNewToken(channelName.getIP()+":"+channelName.getPort());
        } catch (EventException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE,"received snapshot token from a node not registered in the events as direct connection", e);
            //todo decide
        }
        return false;
    }

    private void startNewSnapshot(String snapshotCode, NodeName channelName){
        LoggerManager.getInstance().mutableInfo("Starting new snapshot for " + snapshotCode + " from " + channelName.getIP() + ":" + channelName.getPort(), Optional.of(this.getClass().getName()), Optional.of("startNewSnapshot"));

        List<String> eventNames = EventsBroker.getAllEventChannelNames();
        eventNames.remove(channelName.getIP()+":"+channelName.getPort());
        try {
            Snapshot nSnapshot = new Snapshot(eventNames, snapshotCode, this.connectionManager);
            snapshots.put(snapshotCode, nSnapshot);
        } catch (EventException | IOException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Failed to start snapshot " + snapshotCode, e);
            System.err.println(e.getMessage());
            //todo decide
        }
    }

    public SnapshotState getLastSnapshot() {
        File file = getLastSnapshotFile();
        if(file!=null)LoggerManager.getInstance().mutableInfo("getLastSnapshot: "+ file.getName(), Optional.of(this.getClass().getName()), Optional.of("getLastSnapshot"));
        else LoggerManager.getInstance().mutableInfo("didn't find a snapshot file", Optional.of(this.getClass().getName()), Optional.of("getLastSnapshot"));

        SnapshotState state = null;
        if(file != null){
            state = parseSnapshotFile(file);
        }
        return state;
    }
    private File getLastSnapshotFile(){
        File snapshotsDir = new File(snapshotPath);

        // List all files in the directory that match the pattern
        File[] files = snapshotsDir.listFiles((dir, name) -> name.matches(".*_\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.bin"));

        if (files == null || files.length == 0) {
            return null; // No files found
        }

        // Sort files based on timestamp in their names
        File file = Arrays.stream(files)
                .max(Comparator.comparing(SnapshotManager::extractTimestampFromFilename))
                .orElse(null);
        return file;
    }

    private SnapshotState parseSnapshotFile(File file) {
        byte[] fileContent;
        SnapshotState lastSnapshot = null;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
             lastSnapshot = (SnapshotState) ois.readObject();
        }catch (Exception e){
            //TODO decide
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Failed to parse snapshot file: " + file.getName(), e);
            return null;
        }
        return lastSnapshot;
    }

    private static ZonedDateTime extractTimestampFromFilename(File file) {
        String filename = file.getName();
        // Extract the timestamp part and remove the ".bin"
        String timestampStr = filename.split("_")[1].replace(".bin", "");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        // Parse into LocalDateTime and then combine with the system default zone
        LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, formatter);
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    }





}
