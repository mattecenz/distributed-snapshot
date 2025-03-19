package polimi.ds.dsnapshot;

import org.jetbrains.annotations.TestOnly;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.Messages.ApplicationMessage;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Exception.JavaDSException;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.IOException;
import java.io.Serializable;

public class JavaDistributedSnapshot{
    private static JavaDistributedSnapshot instance;

    private static ConnectionManager connectionManager;
    private static ApplicationLayerInterface applicationLayerInterface;

    //todo find better name

    private JavaDistributedSnapshot() {}

    public synchronized static JavaDistributedSnapshot getInstance() {
        if (instance == null) {
            instance = new JavaDistributedSnapshot();
        }

        return instance;
    }

    public void startSocketConnection(int hostPort) {
        connectionManager = new ConnectionManager(hostPort);
        connectionManager.start();
    }

    public void joinNetwork(ApplicationLayerInterface applicationLayerInterface, String anchorNodeIp, int anchorNodePort) throws JavaDSException {
        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
        try {
            connectionManager.joinNet(anchorNodeIp,anchorNodePort);
        } catch (IOException e) {
            throw new JavaDSException(e.getMessage()); //todo: wrap messages
        }
    }

    @TestOnly
    public void setConnectionManager(ConnectionManager connectionManager) {
        JavaDistributedSnapshot.connectionManager = connectionManager;
    }

    public void leaveNetwork() throws JavaDSException{
        applicationLayerInterface = null;
        try {
            connectionManager.exitNet();
        } catch (IOException e) {
            throw new JavaDSException(e.getMessage()); //todo: wrap messages
        }
    }

    public <T extends Serializable> void sendMessage(T messageContent, boolean requireAck, String destinationIp, int DestinationPort) throws IOException {
        byte[] applicationMessageContente = SerializationUtils.serialize(messageContent);

        ApplicationMessage applicationMessage = new ApplicationMessage(applicationMessageContente, destinationIp, DestinationPort, requireAck);
        connectionManager.sendMessage(applicationMessage, destinationIp, DestinationPort);
    }

    public void ReceiveMessage(Message message){
        ThreadPool.submit(() ->{
            byte [] messageContent = ((ApplicationMessage) message).getApplicationContent();
            applicationLayerInterface.receiveMessage(messageContent);
        });
    }

    public ApplicationLayerInterface getApplicationLayerInterface(){
        return applicationLayerInterface;
    }

    // public void sendMessage()
}
