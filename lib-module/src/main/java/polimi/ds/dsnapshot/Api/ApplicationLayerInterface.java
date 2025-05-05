package polimi.ds.dsnapshot.Api;

import java.io.Serializable;

/**
 * Interface used by the application to exchange information with the library.
 */
public interface ApplicationLayerInterface {

    /**
     * Abstract method which the application needs to implement when a new message is arrived.
     * @param messageContent content of the message. It is a Serializable object which needs to be casted appropriately.
     */
    public abstract void receiveMessage(Serializable messageContent);

    /**
     * Abstract method which the application needs to implement when the library asks for the state to be saved.
     * @return a Serializable object which contains all the application state.
     * @param <T> generic object.
     */
    public abstract <T extends Serializable> T getApplicationState();

    /**
     * Abstract method which the application needs to implement when the library wants to restore the internal state.
     * @param appState Serializable object which contains the new application state to be loaded.
     */
    public abstract void setApplicationState(Serializable appState);

    /**
     * Abstract method which the application needs to implement when a node gracefully leaves the network.
     * @param ip ip of the node who left.
     * @param port port of the node who left.
     */
    public abstract void exitNotify(String ip, int port);
}
