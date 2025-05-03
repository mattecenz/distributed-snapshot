package polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable;

import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.logging.SocketHandler;

public class SerializedSocketHandler implements Serializable{
    private final NodeName nodeName;
    private final boolean isOwner;

    SerializedSocketHandler(NodeName nodeName, boolean isOwner) {
        this.nodeName = nodeName;
        this.isOwner = isOwner;
    }

    public NodeName getNodeName() {
        return nodeName;
    }
    public boolean isOwner() {
        return isOwner;
    }
}
