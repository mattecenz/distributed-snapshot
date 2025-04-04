package polimi.ds.dapplication;

import org.jetbrains.annotations.Debug;
import polimi.ds.dapplication.Message.StringMessage;
import polimi.ds.dsnapshot.ApplicationLayerInterface;

import polimi.ds.dapplication.Message.Message;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.io.Serializable;
import java.util.Optional;

public class AppUtility implements ApplicationLayerInterface {

    // TODO: this is not an atomic operation, so be careful
    // TODO: can this warning cause issues ?
    @Override
    public AppState getApplicationState() {
        return Main.getAppState();
    }

    @Override
    public void receiveMessage(Serializable messageContent) {
        SystemOutTS.println("Received message");

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
                System.err.println("Received message is null or not known");
            }
        }
    }

    @Override
    public void exitNotify(String ip, int port) {
        SystemOutTS.println("A node has left the network: " + ip + ":" + port);
    }
}
