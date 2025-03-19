package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
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

public class SnapshotManager {
    private Dictionary<String, Snapshot> snapshots = new Hashtable<>();
    //TODO: remove snapshot when ends
    private final ConnectionManager connectionManager;

    private static final String snapshotPath = "./snapshots/"; //todo config param

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

    public synchronized void manageSnapshotToken(String snapshotCode, String channelIp, int channelPort) {
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
            snapshots.put(snapshotCode, nSnapshot);
        } catch (EventException | IOException e) {
            System.err.println(e.getMessage());
            //todo decide
        }
    }

    public SnapshotState getLastSnapshot() {
        File file = getLastSnapshotFile();
        SnapshotState state = null;
        if(file != null){
            state = parseSnapshotFile(file);
        }
        return state;
    }
    private File getLastSnapshotFile(){
        File snapshotsDir = new File("./snapshots");

        // List all files in the directory that match the pattern
        File[] files = snapshotsDir.listFiles((dir, name) -> name.matches(".*_\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.bin"));

        if (files == null || files.length == 0) {
            System.err.println("No snapshots found");
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
        try  {
            FileInputStream fis = new FileInputStream(file);
            fileContent = fis.readAllBytes();
            lastSnapshot = (SnapshotState) SerializationUtils.deserialize(fileContent);
            fis.close();
        }catch (Exception e){
            //TODO decide
            System.err.println("execption: " + e);
            return null;
        }
        return lastSnapshot;
    }

    private static ZonedDateTime extractTimestampFromFilename(File file) {
        String filename = file.getName();
        // Extract the timestamp part and remove the ".bin"
        String timestampStr = filename.split("_")[1].replace(".bin", "");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        // Parse into LocalDateTime and then combine with the system default zone
        LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, formatter);
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    }





}
