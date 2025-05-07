package polimi.ds.dsnapshot.Api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.TestOnly;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.Messages.ApplicationMessage;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Exception.ExportedException.*;
import polimi.ds.dsnapshot.Snapshot.SnapshotIdentifier;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.Serializable;
import java.util.Optional;

/**
 * Class which encapsulates the most important objects of the library and is created when the application is started.
 */
public class JavaDistributedSnapshot{
    /**
     * Instance of the object JavaDistributedSnapshot.
     */
    private static JavaDistributedSnapshot instance;
    /**
     * Instance of the connection manager, which manages all the connection related stuff.
     */
    private static ConnectionManager connectionManager;
    /**
     * Instance of the interface visible to the application
     */
    private static ApplicationLayerInterface applicationLayerInterface;

    /**
     * Default constructor.
     */
    private JavaDistributedSnapshot() {}

    /**
     * Getter of the instance of the JavaDistributedSnapshot.
     * It resembles a singleton pattern.
     * @return The instance of the JavaDistributedSnapshot
     */
    public synchronized static JavaDistributedSnapshot getInstance() {
        if (instance == null) {
            instance = new JavaDistributedSnapshot();
        }

        return instance;
    }

    /**
     * Method that starts the connection on a specified socket.
     * @param ip Your IP where you want to open the connection.
     * @param hostPort Your port where you want to open the connection.
     * @param applicationLayerInterface Instance of the interface used by the application to communicate with the library.
     * @throws DSException If some things go wrong when creating the connection.
     */
    public void startSocketConnection(String ip, int hostPort, ApplicationLayerInterface applicationLayerInterface) throws DSException {
        //start log
        LoggerManager.start(hostPort);

        connectionManager = new ConnectionManager(ip, hostPort);
        connectionManager.start();

        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
        LoggerManager.getInstance().mutableInfo("set application interface: " + applicationLayerInterface, Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));
    }

    /**
     * Method that connects the node to another peer.
     * @param anchorNodeIp IP of the node you want to connect to.
     * @param anchorNodePort Port of the node you want to connect to.
     * @throws DSException If some things go wrong when connecting to another node.
     */
    public void joinNetwork(String anchorNodeIp, int anchorNodePort) throws DSException {
        NodeName anchorNodeName = new NodeName(anchorNodeIp, anchorNodePort);
        connectionManager.joinNetwork(anchorNodeName);
    }

    /**
     * Method to leave gracefully the network.
     * @throws DSException If some things go wrong when leaving the network.
     */
    public void leaveNetwork() throws DSException{
        applicationLayerInterface = null;
        connectionManager.exitNetwork();
    }

    /**
     * Method that sends a message from the application over the network.
     * @param messageContent Serializable object which encapsulates the message sent by the application.
     * @param destinationIp IP of the destination of the message.
     * @param destinationPort Port of the destination of the message.
     * @throws DSException If something goes wrong while sending the message.
     */
    public void sendMessage(Serializable messageContent, String destinationIp, int destinationPort) throws DSException {
        NodeName destinationNodeName = new NodeName(destinationIp, destinationPort);

        connectionManager.sendMessage(messageContent, destinationNodeName);
    }

    /**
     * Method that lets the user reconnect to his original parent.
     * @throws DSException If something goes wrong while reconnecting.
     */
    public void reconnect() throws DSException {
        connectionManager.reconnectToAnchor();
    }

    /**
     * Method which lets the user start a new snapshot.
     */
    public void startNewSnapshot(){
        connectionManager.startNewSnapshot();
    }

    /**
     * Method which lets the user restore an existing snapshot.
     * @param snapshotId Unique code associated to each snapshot.
     * @param snapshotIp IP of the creator of the snapshot.
     * @param snapshotPort Port of the creator of the snapshot.
     * @throws DSSnapshotRestoreRemoteException If something goes wrong in the remote node.
     * @throws DSSnapshotRestoreLocalException If something goes wrong in the local node.
     */
    public void restoreSnapshot(String snapshotId, String snapshotIp, int snapshotPort) throws DSSnapshotRestoreRemoteException, DSSnapshotRestoreLocalException {
        SnapshotIdentifier snapshotIdentifier = new SnapshotIdentifier(new NodeName(snapshotIp,snapshotPort),snapshotId);
        connectionManager.startSnapshotRestoreProcedure(snapshotIdentifier);
    }

    /**
     * Method which lets the user see al the available snapshots of the node.
     * @return A pretty formatted string with all the available snapshots.
     */
    public String getAvailableSnapshots(){

        return connectionManager.getAvailableSnapshots();
    }

    /**
     * Method used only internally to the library to invoke the receipt of a message.
     * It creates a new thread which calls the receive message method of the application interface.
     * @param callbackContent Callback content which contains the message to be forwarded to the application.
     */
    @ApiStatus.Internal
    public void ReceiveMessage(CallbackContent callbackContent){
        ThreadPool.submit(() ->{
            LoggerManager.getInstance().mutableInfo("forward msg to app", Optional.of(this.getClass().getName()), Optional.of("ReceiveMessage"));
            Serializable messageContent = ((ApplicationMessage) callbackContent.getCallBackMessage()).getApplicationContent();
            applicationLayerInterface.receiveMessage(messageContent);
        });
    }

    /**
     * Internal getter of the application interface.
     * @return The application layer interface.
     */
    @ApiStatus.Internal
    public ApplicationLayerInterface getApplicationLayerInterface(){
        return applicationLayerInterface;
    }

    /**
     * Internal method which notifies the application interface that a node has left the application.
     * @param nodeName Name of the node who left.
     */
    @ApiStatus.Internal
    public void applicationExitNotify(NodeName nodeName){
        applicationLayerInterface.exitNotify(nodeName.getIP(), nodeName.getPort());
    }

    /**
     * Testing method used for manually setting a dummy connection manager
     * @param connectionManager The connection manager to be set.
     */
    @TestOnly
    public void setConnectionManager(ConnectionManager connectionManager) {
        JavaDistributedSnapshot.connectionManager = connectionManager;
    }

    /**
     * Testing method used for manually setting a dummy application interface.
     * @param applicationLayerInterface The application interface to be set.
     */
    @TestOnly
    public void setApplicationLayerInterface(ApplicationLayerInterface applicationLayerInterface) {
        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
    }
}
