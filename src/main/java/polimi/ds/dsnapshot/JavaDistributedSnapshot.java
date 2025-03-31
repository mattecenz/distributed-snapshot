package polimi.ds.dsnapshot;

import org.jetbrains.annotations.TestOnly;
import org.w3c.dom.Node;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.Messages.ApplicationMessage;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Exception.JavaDSException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.IOException;
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

    //todo find better name
    public void startSocketConnection(int hostPort, ApplicationLayerInterface applicationLayerInterface) {
        //start log
        LoggerManager.start(hostPort);

        connectionManager = new ConnectionManager(hostPort);
        connectionManager.start();

        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
        LoggerManager.getInstance().mutableInfo("set application interface: " + applicationLayerInterface, Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));
    }

    public void joinNetwork(String anchorNodeIp, int anchorNodePort) throws JavaDSException {
        try {
            NodeName anchorNodeName = new NodeName(anchorNodeIp, anchorNodePort);
            connectionManager.joinNetwork(anchorNodeName);
        } catch (IOException e) {
            throw new JavaDSException(e.getMessage()); //todo: wrap messages
        }
    }

    @TestOnly
    public void setConnectionManager(ConnectionManager connectionManager) {
        JavaDistributedSnapshot.connectionManager = connectionManager;
    }
    @TestOnly
    public void setApplicationLayerInterface(ApplicationLayerInterface applicationLayerInterface) {
        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
    }

    public void leaveNetwork() throws JavaDSException{
        applicationLayerInterface = null;
        try {
            connectionManager.exitNetwork();
        } catch (IOException e) {
            throw new JavaDSException(e.getMessage()); //todo: wrap messages
        }
    }

    public <T extends Serializable> void sendMessage(T messageContent, boolean requireAck, String destinationIp, int destinationPort) throws IOException {
        byte[] applicationMessageContente = SerializationUtils.serialize(messageContent);

        NodeName destinationNodeName = new NodeName(destinationIp, destinationPort);

        ApplicationMessage applicationMessage = new ApplicationMessage(applicationMessageContente, destinationNodeName, requireAck);
        connectionManager.sendMessage(applicationMessage, destinationNodeName);
    }

    public void ReceiveMessage(CallbackContent callbackContent){
        ThreadPool.submit(() ->{
            LoggerManager.getInstance().mutableInfo("forward msg to app", Optional.of(this.getClass().getName()), Optional.of("ReceiveMessage"));
            byte [] messageContent = ((ApplicationMessage) callbackContent.getCallBackMessage()).getApplicationContent();
            applicationLayerInterface.receiveMessage(messageContent);
        });
    }

    public ApplicationLayerInterface getApplicationLayerInterface(){
        return applicationLayerInterface;
    }

    public void startNewSnapshot(){
        connectionManager.startNewSnapshot();
    }
    // public void sendMessage()
}
