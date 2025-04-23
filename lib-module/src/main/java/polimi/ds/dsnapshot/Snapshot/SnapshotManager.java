package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.Messages.Snapshot.RestoreSnapshotRequest;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Exception.SpanningTreeNoAnchorNodeException;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

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

    private Map<SnapshotIdentifier,SnapshotState> lastSnapshotState = new Hashtable<>(); //creator port as key

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

    // <editor-fold desc="Snapshot procedure">
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
            Snapshot nSnapshot = new Snapshot(eventNames, snapshotCode, this.connectionManager, this.connectionManager.getName().getPort());
            snapshots.put(snapshotCode, nSnapshot);
        } catch (EventException | IOException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Failed to start snapshot " + snapshotCode, e);
            System.err.println(e.getMessage());
            //todo decide
        }
    }

    public SnapshotState getLastSnapshot(int hostPort) {
        File file = getLastSnapshotFile(hostPort);
        if(file!=null)LoggerManager.getInstance().mutableInfo("getLastSnapshot: "+ file.getName(), Optional.of(this.getClass().getName()), Optional.of("getLastSnapshot"));
        else LoggerManager.getInstance().mutableInfo("didn't find a snapshot file", Optional.of(this.getClass().getName()), Optional.of("getLastSnapshot"));

        SnapshotState state = null;
        if(file != null){
            state = parseSnapshotFile(file);
        }
        return state;
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
        System.out.println();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        // Parse into LocalDateTime and then combine with the system default zone
        LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, formatter);
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    }
    // </editor-fold>

    // <editor-fold desc="restore Snapshot procedure">
    public synchronized boolean reEnteringNodeValidateSnapshotRequest(SnapshotIdentifier snapshotIdentifier) {
        LoggerManager.getInstance().mutableInfo("starting validate snapshot request on re-entering node", Optional.of(this.getClass().getName()), Optional.of("reEnteringNodeValidateSnapshotRequest"));
        if (!lastSnapshotState.isEmpty()) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated due to the presence of a competing snapshot");
            return false;
        }

        File file = getLastSnapshotFile(snapshotIdentifier, this.connectionManager.getName().getPort());
        SnapshotState state = null;

        if(file != null){
            state = parseSnapshotFile(file);
        }

        //no snapshot file found
        if(state == null) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated, snapshot file can't be found");
            return false;
        }

        lastSnapshotState.put(snapshotIdentifier, state);
        LoggerManager.getInstance().mutableInfo("success in validate snapshot request on re-entering node", Optional.of(this.getClass().getName()), Optional.of("reEnteringNodeValidateSnapshotRequest"));
        return true;
    }

    public synchronized boolean validateSnapshotRequest(RestoreSnapshotRequest resetSnapshotRequest) {
        LoggerManager.getInstance().mutableInfo("starting validate snapshot request", Optional.of(this.getClass().getName()), Optional.of("validateSnapshotRequest"));
        if (!lastSnapshotState.isEmpty()) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated due to the presence of a competing snapshot");
            return false;
        }
        File file = getLastSnapshotFile(resetSnapshotRequest.getSnapshotIdentifier(), this.connectionManager.getName().getPort());
        SnapshotState state = null;

        if(file != null){
            state = parseSnapshotFile(file);
        }

        //no snapshot file found
        if(state == null) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated, snapshot file can't be found");
            return false;
        }

        //validate anchor node
        try {
            if(state.getAnchorNode()!= null && !Objects.equals(state.getAnchorNode(), connectionManager.getSpt().getAnchorNodeHandler().getRemoteNodeName())){
                LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated due inconsistent anchor node");
                return false;
            }
        } catch (SpanningTreeNoAnchorNodeException e) {
            LoggerManager.getInstance().mutableInfo("current anchor node is null", Optional.of(this.getClass().getName()), Optional.of("validateSnapshotRequest"));
            if(state.getAnchorNode()!= null && !state.getAnchorNode().equals(resetSnapshotRequest.getSnapshotIdentifier().getSnapshotCreatorName())) { //the current anchor node can be null if the current node is child of the re-entering node
                LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated due inconsistent anchor node");
                return false;
            }
        }

        //validate direct connection in the routing table
        List<NodeName> ignoredList = new ArrayList<>();
        ignoredList.add(resetSnapshotRequest.getSnapshotIdentifier().getSnapshotCreatorName());
        if(!connectionManager.getRoutingTable().SerializedValidation(state.getRoutingTable(),ignoredList)) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated due inconsistent routing table");
            return false;
        }
        lastSnapshotState.put(resetSnapshotRequest.getSnapshotIdentifier(), state);
        LoggerManager.getInstance().mutableInfo("success in validate snapshot request", Optional.of(this.getClass().getName()), Optional.of("validateSnapshotRequest"));
        return true;
    }
    // </editor-fold>


    private File getLastSnapshotFile(SnapshotIdentifier snapshotIdentifier, int hostPort){
        File snapshotsDir = new File(snapshotPath);

        // List all files in the directory that match the pattern
        File[] files = snapshotsDir.listFiles((dir, name) ->
                name.matches(".*_\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.bin") &&
                        name.contains(snapshotIdentifier.getSnapshotId() + "-" + snapshotIdentifier.getSnapshotCreatorName().getIP() + "-" + snapshotIdentifier.getSnapshotCreatorName().getPort()+"-" + hostPort + "_")
        );

        if (files == null || files.length == 0) {
            return null; // No files found
        }

        // Sort files based on timestamp in their names
        File file = Arrays.stream(files)
                .max(Comparator.comparing(SnapshotManager::extractTimestampFromFilename))
                .orElse(null);
        return file;
    }

    private File getLastSnapshotFile(int hostPort){
        File snapshotsDir = new File(snapshotPath);

        // List all files in the directory that match the pattern
        File[] files = snapshotsDir.listFiles((dir, name) ->
                name.matches(".*_\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.bin") &&
                        name.contains("-" + hostPort + "_")
        );

        if (files == null || files.length == 0) {
            return null; // No files found
        }

        // Sort files based on timestamp in their names
        File file = Arrays.stream(files)
                .max(Comparator.comparing(SnapshotManager::extractTimestampFromFilename))
                .orElse(null);
        return file;
    }



}
