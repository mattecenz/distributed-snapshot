package polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable;

import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.logging.SocketHandler;

/**
 * TODO: change name of the class to Serializable ?
 * Serializable version of the socket handler.
 * Since not all its components are Serializable we need a more compact version to store on disk the important information.
 */
public class SerializedSocketHandler implements Serializable{
    /**
     * Name of the node connected to the socket handler.
     */
    private final NodeName nodeName;
    /**
     * Boolean which represents if this node is the owner (i.e. created first the connection) of the socket.
     */
    private final boolean isOwner;

    /**
     * Constructor of the serializable socket handler.
     * @param nodeName Name of the node connected
     * @param isOwner Boolean which represents the ownership of the handler.
     */
    SerializedSocketHandler(NodeName nodeName, boolean isOwner) {
        this.nodeName = nodeName;
        this.isOwner = isOwner;
    }

    /**
     * Getter of the remote node name.
     * @return The node name.
     */
    public NodeName getNodeName() {
        return nodeName;
    }

    /**
     * Getter of the ownership value.
     * @return Boolean representing the ownership of the handler.
     */
    public boolean isOwner() {
        return isOwner;
    }
}
