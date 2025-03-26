package polimi.ds.dapplication;

import polimi.ds.dapplication.Message.StringMessage;
import polimi.ds.dsnapshot.ApplicationLayerInterface;

import polimi.ds.dapplication.Message.Message;

import java.io.Serializable;

public class AppUtility implements ApplicationLayerInterface {

    // TODO: serialize the state
    @Override
    public byte[] getApplicationState() {
        return new byte[0];
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
