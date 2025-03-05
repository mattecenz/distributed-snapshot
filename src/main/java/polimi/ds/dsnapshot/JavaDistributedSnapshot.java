package polimi.ds.dsnapshot;

import polimi.ds.dsnapshot.Connection.ConnectionManager;

import java.io.IOException;

public class JavaDistributedSnapshot {
    private static ConnectionManager connectionManager;
    private static ApplicationLayerInterface applicationLayerInterface;

    //todo find better name
    static public void startSocketConnection(int hostPort) {
        connectionManager = new ConnectionManager(hostPort);
        connectionManager.start();
    }

    static public void joinNetwork(ApplicationLayerInterface applicationLayerInterface, String anchorNodeIp, int anchorNodePort){
        JavaDistributedSnapshot.applicationLayerInterface = applicationLayerInterface;
        try {
            connectionManager.joinNet(anchorNodeIp,anchorNodePort);
        } catch (IOException e) {
            //todo: manage exception
        }
    }

    static public void leaveNetwork(){
        applicationLayerInterface = null;
        try {
            connectionManager.exitNet();
        } catch (IOException e) {
            //todo: manage exception
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
