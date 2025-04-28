package polimi.ds.dsnapshot.Connection.SnashotSerializable.SPT;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
public class SerializableSpanningTree implements Serializable {
    private final NodeName anchorNodeName;
    private final List<NodeName> childrenNames;

    SerializableSpanningTree(ClientSocketHandler anchorNodeHandler, List<ClientSocketHandler> children) {
        this.anchorNodeName = anchorNodeHandler.getRemoteNodeName();
        this.childrenNames = new ArrayList<>();

        for (ClientSocketHandler child : children) {
            childrenNames.add(child.getRemoteNodeName());
        }
    }

    public List<NodeName> getChildrenNames() {
        return childrenNames;
    }

    public NodeName getAnchorNodeName() {
        return anchorNodeName;
    }
}
