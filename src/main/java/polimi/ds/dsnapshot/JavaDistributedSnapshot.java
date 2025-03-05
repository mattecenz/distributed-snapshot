package polimi.ds.dsnapshot;

import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Exception.JavaDSException;

import java.io.IOException;

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

    static public void leaveNetwork() throws JavaDSException{
        applicationLayerInterface = null;
        try {
            connectionManager.exitNet();
        } catch (IOException e) {
            throw new JavaDSException(e.getMessage()); //todo: wrap messages
        }
    }

    static public void sendMessage(byte[] messageContent, boolean requireAck){
        //todo
    }

    static public void ReceiveMessage(byte[] messageContent){
        applicationLayerInterface.receiveMessage(messageContent);
    }

    // public void sendMessage()
}
