package polimi.ds.dapplication;

import polimi.ds.dsnapshot.ApplicationLayerInterface;

public class AppUtility implements ApplicationLayerInterface {
    @Override
    public void receiveMessage(byte[] messageContent) {
        System.out.println("Received message");
    }

    @Override
    public byte[] getApplicationState() {
        return new byte[0];
    }
}
