package polimi.ds.dapplication;

import polimi.ds.dapplication.Message.StringMessage;
import polimi.ds.dsnapshot.ApplicationLayerInterface;

import polimi.ds.dapplication.Message.Message;

import java.io.Serializable;

public class AppUtility implements ApplicationLayerInterface {

    // TODO: this is not an atomic operation, so be careful
    // TODO: can this warning cause issues ?
    @Override
    public AppState getApplicationState() {
        return Main.getAppState();
    }

    @Override
    public <T extends Serializable> void receiveMessage(T messageContent) {
        System.out.println("Received message");

        // Perform the cast. It can be always done if we assume the application sends messages
        Message m = (Message) messageContent;

        switch (m.getMessageID()){
            case MESSAGE_STRING -> {
                StringMessage sm = (StringMessage) m;
                SystemOutTS.println("Somebody sent a message : " + sm.getMessage());
            }
            case MESSAGE_NOT_IMPLEMENTED -> {
                System.err.println("Received message not implemented");
            }
            case null, default -> {
                System.err.println("Received message not implemented");
            }
        }
    }
}
