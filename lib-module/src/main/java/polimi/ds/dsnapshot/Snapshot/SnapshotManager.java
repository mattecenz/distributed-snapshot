package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Api.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Api.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.Messages.Snapshot.RestoreSnapshotRequest;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Exception.Snapshot.Snapshot2PCException;
import polimi.ds.dsnapshot.Exception.Snapshot.SnapshotRestoreException;
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

/**
 * Class which manages the whole snapshot procedure.
 */
public class SnapshotManager {
    /**
     * Dictionary of the currently active snapshots.
     */
    private Dictionary<String, Snapshot> snapshots = new Hashtable<>();
    //TODO: remove snapshot when ends
    /**
     * Reference to the connection manager.
     */
    private final ConnectionManager connectionManager;
    /**
     * Internal path where the snapshots are saved.
     */
    private static final String snapshotPath = Config.getString("snapshot.path");
    /**
     * Reference to the application layer interface.
     */
    private ApplicationLayerInterface applicationLayerInterface;
    /**
     * TODO: shouldn't it be an HashMap ? Does it change something ? Idk
     * Map to save for each identifier of the snapshot the last saved state.
     */
    private Map<SnapshotIdentifier,SnapshotState> lastSnapshotState = new Hashtable<>(); //creator port as key

    /**
     * Constructor of the snapshot manager.
     * @param connectionManager Reference to the connection manager.
     */
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
    /**
     * Method called when a token has been received from a socket connection.
     * If the token is the first received then the node has to initiate the snapshot procedure,
     * else stop listening from the channel.
     * It is a synchronized procedure.
     * @param snapshotCode Code of the snapshot associated to the token.
     * @param channelName Name of the channel from where the token has been received.
     * @return True if a new snapshot has been started.
     */
    public synchronized boolean manageSnapshotToken(String snapshotCode, NodeName channelName) {
        if(applicationLayerInterface==null) {
            JavaDistributedSnapshot javaDistributedSnapshot = JavaDistributedSnapshot.getInstance();
            applicationLayerInterface = javaDistributedSnapshot.getApplicationLayerInterface();
        }

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

    /**
     * Method called when the user wants to start a new snapshot, or a new snapshot token has been received.
     * It will initiate the snapshot by preparing all the input channels and by sending the token along the sockets.
     * @param snapshotCode Unique string containing the snapshot code.
     * @param channelName Name from where the snapshot channel has been received.
     */
    private void startNewSnapshot(String snapshotCode, NodeName channelName){
        LoggerManager.getInstance().mutableInfo("Starting new snapshot for " + snapshotCode + " from " + channelName.getIP() + ":" + channelName.getPort(), Optional.of(this.getClass().getName()), Optional.of("startNewSnapshot"));

        List<String> eventNames = EventsBroker.getAllEventChannelNames();
        eventNames.remove(channelName.getIP()+":"+channelName.getPort());
        try {
            Snapshot nSnapshot = new Snapshot(eventNames, snapshotCode, this.connectionManager, this.connectionManager.getName().getPort(),applicationLayerInterface);
            snapshots.put(snapshotCode, nSnapshot);
        } catch (EventException | IOException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Failed to start snapshot " + snapshotCode, e);
            System.err.println(e.getMessage());
            //todo decide
        }
    }

    /**
     * //TODO: isnt the Optional<> usage here optimal ?
     * Method called to retrieve the last snapshot (in term of local time) saved in the directory.
     * @param hostPort Host port associated to the state that needs to be retrieved.
     * @return A valid snapshot state if it was found, else null.
     */
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

    /**
     * Method to manually parse the snapshot file passed in input.
     * It reads the file and converts it to a valid snapshot state.
     * @param file Reference to the file to be read.
     * @return A valid snapshot state if everything went well, else null.
     */
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

    /**
     * //TODO: remind me to use this method when printing also the file names to the user.
     * Method called when extracting the information about the time at which the file was saved.
     * @param file Reference to the file to be analyzed.
     * @return A valid Date and Time format.
     */
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

    /**
     * Method called when trying to understand if it is possible to restore the snapshot.
     * Many things can go wrong,
     * there may be another snapshot which is already being restored,
     * the snapshot file is not found,
     * the library is not in a valid state for the restore.
     * It is a synchronized operation.
     * @param snapshotIdentifier Identifier of the snapshot to be restored.
     * @throws Snapshot2PCException If some error occurred when the 2PC was in act.
     */
    public synchronized void reEnteringNodeValidateSnapshotRequest(SnapshotIdentifier snapshotIdentifier) throws Snapshot2PCException {
        LoggerManager.getInstance().mutableInfo("starting validate snapshot request on re-entering node", Optional.of(this.getClass().getName()), Optional.of("reEnteringNodeValidateSnapshotRequest"));
        if (!lastSnapshotState.isEmpty()) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated due to the presence of a competing snapshot");
            throw new Snapshot2PCException("Snapshot can't be validated due to the presence of a competing snapshot");
        }

        File file = getLastSnapshotFile(snapshotIdentifier, this.connectionManager.getName().getPort());
        SnapshotState state = null;

        if(file != null){
            state = parseSnapshotFile(file);
        }

        //no snapshot file found
        if(state == null) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated, snapshot file can't be found");
            throw new Snapshot2PCException("Snapshot can't be validated, snapshot file can't be found");
        }

        if(!connectionManager.getSpt().serializedValidation(state.getSerializableSpanningTree())) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Snapshot can't be validated, inconsistent spanning tree");
            throw new Snapshot2PCException("Snapshot can't be validated, inconsistent spanning tree");
        }

        lastSnapshotState.put(snapshotIdentifier, state);
        LoggerManager.getInstance().mutableInfo("success in validate snapshot request on re-entering node", Optional.of(this.getClass().getName()), Optional.of("reEnteringNodeValidateSnapshotRequest"));

        //validate spt
    }

    /**
     * Method to validate the restore request of a snapshot.
     * It is a synchronized operation.
     * @param resetSnapshotRequest Message request from another node to restore the snapshot.
     * @throws Snapshot2PCException If the snapshot cannot be restored due to an error.
     */
    public synchronized void validateSnapshotRequest(RestoreSnapshotRequest resetSnapshotRequest) throws Snapshot2PCException {
        reEnteringNodeValidateSnapshotRequest(resetSnapshotRequest.getSnapshotIdentifier());
    }

    /**
     * Method to remove the snapshot request from the active list.
     * It is a synchronized operation.
     * @param snapshotIdentifier Unique identifier of the snapshot to be removed.
     */
    public synchronized void removeSnapshotRequest(SnapshotIdentifier snapshotIdentifier) {
        LoggerManager.getInstance().mutableInfo("abort restore procedure by clearing snapshot last state", Optional.of(this.getClass().getName()), Optional.of("removeSnapshotRequest"));
        lastSnapshotState.remove(snapshotIdentifier);
    }

    /**
     * Method to retrieve the routing table from the snapshot passed in input.
     * It is a synchronized operation.
     * @param snapshotIdentifier Unique identifier of the snapshot to be restored.
     * @return List of socket handlers contained in the restored routing table.
     * @throws SnapshotRestoreException If some error occurred while restoring the snapshot.
     */
    public synchronized List<ClientSocketHandler> restoreSnapshotRoutingTable(SnapshotIdentifier snapshotIdentifier) throws SnapshotRestoreException {
        SnapshotState state = lastSnapshotState.get(snapshotIdentifier);
        if(state == null) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "try to restore a snapshot without agreement");
            throw new SnapshotRestoreException("try to restore a snapshot without agreement");
        }

        return connectionManager.getRoutingTable().fromSerialize(state.getRoutingTable(), connectionManager);
    }

    /**
     * Method called when the network has agreed on a snapshot to be restored.
     * It will restore the snapshot by executing the incoming messages and it will restore the routing table connections.
     * It is an atomic operation.
     * @param snapshotIdentifier Unique identifier of the snapshot to restore.
     * @throws EventException If some exception in the event channels has happened.
     * @throws SnapshotRestoreException If there was an error restoring the snapshot.
     */
    public synchronized void restoreSnapshot(SnapshotIdentifier snapshotIdentifier) throws EventException, SnapshotRestoreException {
        LoggerManager.getInstance().mutableInfo("starting restore the snapshot after 2pc agreement", Optional.of(this.getClass().getName()), Optional.of("restoreSnapshot"));
        SnapshotState state = lastSnapshotState.get(snapshotIdentifier);
        if(state == null) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "try to restore a snapshot without agreement");
            throw new SnapshotRestoreException("try to restore a snapshot without agreement");
        }
        lastSnapshotState.remove(snapshotIdentifier);

        if(applicationLayerInterface==null) applicationLayerInterface = JavaDistributedSnapshot.getInstance().getApplicationLayerInterface();
        applicationLayerInterface.setApplicationState(state.getApplicationState());
        LoggerManager.getInstance().mutableInfo("application state restored.", Optional.of(this.getClass().getName()), Optional.of("restoreSnapshot"));

        Map<String, Event> events = new HashMap<>();
        for(CallbackContentWithName callbackContent : state.getMessageInputStack()){
            Event event = null;
            if(events.containsKey(callbackContent.getEventName())) event = events.get(callbackContent.getEventName());
            else {
                event = EventsBroker.getEventChannel(callbackContent.getEventName());
                events.put(callbackContent.getEventName(), event);
            }
            event.publish(callbackContent.getCallBackMessage());
        }
    }

    // </editor-fold>

    /**
     * Method to retrieve the last snapshot file by looking at its identifier.
     * @param snapshotIdentifier Unique identifier of the snapshot.
     * @param hostPort Port of the host associated with the snapshot.
     * @return The newest snapshot file.
     */
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

    /**
     * Method to retrieve the latest snapshot file without looking at the identifier.
     * @param hostPort Port of the host associated with the snapshot.
     * @return The newest snapshot file.
     */
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

    /**
     * Method called by the application to view all the snapshots saved.
     * @param nodeName Name of the node for which the files will be listed.
     * @return A string containing all the file information (unique code, snapshot creator, timestamp of creation).
     */
    public String getAllSnapshotsOfNode(NodeName nodeName){
        File snapshotDir = new File(snapshotPath);

        // Match all the files containing my name

        File[] myFiles = snapshotDir.listFiles((dir, name) ->
                name.split("-")[3].split("_")[0].equals(Integer.toString(nodeName.getPort())));

        // Convert the files into a string

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Code \t\t Owner Name \t Time saved \n");

        for(File file : myFiles){
            String[] fileNamePieces = file.getName().split("-");
            stringBuilder.append(fileNamePieces[0])
                    .append("\t")
                    .append(fileNamePieces[1])
                    .append(":")
                    .append(fileNamePieces[2])
                    .append("\t")
                    .append(fileNamePieces[5])
                    .append("/")
                    .append(fileNamePieces[4])
                    .append("/")
                    .append(fileNamePieces[3].split("_")[1])
                    .append(" ")
                    .append(fileNamePieces[6])
                    .append(":")
                    .append(fileNamePieces[7])
                    .append(":")
                    .append(fileNamePieces[8].split("\\.")[0])
                    .append("\n");
        }

        return stringBuilder.toString();
    }

}
