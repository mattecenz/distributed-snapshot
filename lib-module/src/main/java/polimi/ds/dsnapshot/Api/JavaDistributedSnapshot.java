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

public class JavaDistributedSnapshot{
    private static JavaDistributedSnapshot instance;
    private static ConnectionManager connectionManager;
    private static ApplicationLayerInterface applicationLayerInterface;

    private JavaDistributedSnapshot() {}

    public synchronized static JavaDistributedSnapshot getInstance() {
        if (instance == null) {
            instance = new JavaDistributedSnapshot();
        }

        return instance;
    }

    public void startSocketConnection(String ip, int hostPort, ApplicationLayerInterface applicationLayerInterface) throws DSException {
        //start log
        LoggerManager.start(hostPort);

        connectionManager = new ConnectionManager(ip, hostPort);
        connectionManager.start();

        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
        LoggerManager.getInstance().mutableInfo("set application interface: " + applicationLayerInterface, Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));
    }

    public void joinNetwork(String anchorNodeIp, int anchorNodePort) throws DSException {
        NodeName anchorNodeName = new NodeName(anchorNodeIp, anchorNodePort);
        connectionManager.joinNetwork(anchorNodeName);
    }

    public void leaveNetwork() throws DSException{
        applicationLayerInterface = null;
        connectionManager.exitNetwork();
    }

    public void sendMessage(Serializable messageContent, String destinationIp, int destinationPort) throws DSException {
        NodeName destinationNodeName = new NodeName(destinationIp, destinationPort);

        connectionManager.sendMessage(messageContent, destinationNodeName);
    }

    public void reconnect() throws DSException {
        connectionManager.reconnectToAnchor();
    }

    public void startNewSnapshot(){
        connectionManager.startNewSnapshot();
    }

    public void restoreSnapshot(String snapshotId, String snapshotIp, int snapshotPort) throws DSSnapshotRestoreRemoteException, DSSnapshotRestoreLocalException {
        SnapshotIdentifier snapshotIdentifier = new SnapshotIdentifier(new NodeName(snapshotIp,snapshotPort),snapshotId);
        connectionManager.startSnapshotRestoreProcedure(snapshotIdentifier);
    }

    public String getAvailableSnapshots(){

        return connectionManager.getAvailableSnapshots();
    }

    @ApiStatus.Internal
    public void ReceiveMessage(CallbackContent callbackContent){
        ThreadPool.submit(() ->{
            LoggerManager.getInstance().mutableInfo("forward msg to app", Optional.of(this.getClass().getName()), Optional.of("ReceiveMessage"));
            Serializable messageContent = ((ApplicationMessage) callbackContent.getCallBackMessage()).getApplicationContent();
            applicationLayerInterface.receiveMessage(messageContent);
        });
    }
    @ApiStatus.Internal
    public ApplicationLayerInterface getApplicationLayerInterface(){
        return applicationLayerInterface;
    }
    @ApiStatus.Internal
    public void applicationExitNotify(NodeName nodeName){
        applicationLayerInterface.exitNotify(nodeName.getIP(), nodeName.getPort());
    }
    @TestOnly
    public void setConnectionManager(ConnectionManager connectionManager) {
        JavaDistributedSnapshot.connectionManager = connectionManager;
    }
    @TestOnly
    public void setApplicationLayerInterface(ApplicationLayerInterface applicationLayerInterface) {
        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
    }
}
