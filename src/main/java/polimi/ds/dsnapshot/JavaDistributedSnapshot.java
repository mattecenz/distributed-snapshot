package polimi.ds.dsnapshot;

import org.jetbrains.annotations.TestOnly;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.Messages.ApplicationMessage;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Exception.JavaDSException;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;

public class JavaDistributedSnapshot {
    private static ConnectionManager connectionManager;
    private static ApplicationLayerInterface applicationLayerInterface;

    //todo find better name
    static public void startSocketConnection(int hostPort) {
        connectionManager = new ConnectionManager(hostPort);
        connectionManager.start();
    }

    static public void joinNetwork(ApplicationLayerInterface applicationLayerInterface, String anchorNodeIp, int anchorNodePort) throws JavaDSException {
        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
        try {
            connectionManager.joinNet(anchorNodeIp,anchorNodePort);
        } catch (IOException e) {
            throw new JavaDSException(e.getMessage()); //todo: wrap messages
        }
    }

    @TestOnly
    public static void setConnectionManager(ConnectionManager connectionManager) {
        JavaDistributedSnapshot.connectionManager = connectionManager;
    }

    static public void leaveNetwork() throws JavaDSException{
        applicationLayerInterface = null;
        try {
            connectionManager.exitNet();
        } catch (IOException e) {
            throw new JavaDSException(e.getMessage()); //todo: wrap messages
        }
    }

    static public <T extends Serializable> void sendMessage(T messageContent, boolean requireAck, String destinationIp, int DestinationPort) throws IOException {
        byte[] applicationMessageContente = SerializationUtils.serialize(messageContent);

        ApplicationMessage applicationMessage = new ApplicationMessage(applicationMessageContente, destinationIp, DestinationPort, requireAck);
        connectionManager.sendMessage(applicationMessage, destinationIp, DestinationPort);
    }

    static public void ReceiveMessage(Message message){
        byte [] messageContent = ((ApplicationMessage) message).getApplicationContent();
        applicationLayerInterface.receiveMessage(messageContent);
    }

    static public ApplicationLayerInterface getApplicationLayerInterface(){
        return applicationLayerInterface;
    }

    // public void sendMessage()
}
