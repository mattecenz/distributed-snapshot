package polimi.ds.dapplication;

import polimi.ds.dapplication.Message.StringMessage;
import polimi.ds.dsnapshot.Api.ApplicationLayerInterface;

import polimi.ds.dapplication.Message.Message;

import java.io.Serializable;

/**
 * Example of a class which implements the interface exposed by the library.
 */
public class AppUtility implements ApplicationLayerInterface {

    /**
     * Override of the interface method to retrieve the application state.
     * @return A Serializable object which corresponds to the application state at this moment.
     */
    // TODO: this is not an atomic operation, so be careful
    // TODO: can this warning cause issues ?
    @Override
    public AppState getApplicationState() {
        SystemOutTS.println("Collecting state of the application...");
        return Main.getAppState();
    }

    /**
     * Override of the interface method which notifies the application when a message is received.
     * @param messageContent Serializable object which the application needs to cast accordingly
     *                       to read the message received.
     */
    @Override
    public void receiveMessage(Serializable messageContent) {
        SystemOutTS.println("Received message");

        // Perform the cast. It can be always done if we assume the application sends messages
        Message m = (Message) messageContent;

        switch (m.getMessageID()){
            case MESSAGE_STRING -> {
                StringMessage sm = (StringMessage) m;
                SystemOutTS.println("Somebody sent a message : " + sm.getMessage());
                // Add the message to the state
                Main.getAppState().appendMessage(sm.getMessage());
            }
            case MESSAGE_NOT_IMPLEMENTED -> {
                System.err.println("Received message not implemented");
            }
            case null, default -> {
                System.err.println("Received message is null or not known");
            }
        }
    }

    /**
     * Override of the interface method which restores the application state.
     * @param appState Serializable object which needs to be casted accordingly to a valid application state.
     */
    @Override
    public void setApplicationState(Serializable appState) {
        SystemOutTS.println("Snapshot state restored!");
        Main.getAppState().restoreAppState(appState);
    }

    /**
     * Override of the interface method which notifies the application when a node has left gracefully the network.
     * @param ip IP of the disconnected node.
     * @param port Port of the disconnected node.
     */
    @Override
    public void exitNotify(String ip, int port) {
        SystemOutTS.println("A node has left the network: " + ip + ":" + port);
    }
}
