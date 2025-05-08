package polimi.ds.dsnapshot.Connection.SnashotSerializable.SPT;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serializable version of the spanning tree.
 * Since the original class is not serializable we need a simplified version with all the important information
 * to be saved in the disk.
 */
public class SerializableSpanningTree implements Serializable {
    /**
     * Name of the anchor of the spt of the node.
     */
    private final NodeName anchorNodeName;
    /**
     * List of names containing the children of the node.
     */
    private final List<NodeName> childrenNames;

    /**
     * Constructor of the serializable spt.
     * @param anchorNodeHandler Optional containing the socket handler of the anchor node.
     * @param children List of children socket handlers.
     */
    SerializableSpanningTree(Optional<ClientSocketHandler> anchorNodeHandler, List<ClientSocketHandler> children) {
        this.anchorNodeName = anchorNodeHandler
                .map(ClientSocketHandler::getRemoteNodeName)
                .orElse(null);
        this.childrenNames = new ArrayList<>();

        for (ClientSocketHandler child : children) {
            childrenNames.add(child.getRemoteNodeName());
        }
    }

    /**
     * Getter of the children names.
     * @return A list of names of all the children nodes.
     */
    public List<NodeName> getChildrenNames() {
        return childrenNames;
    }

    /**
     * Getter of the anchor node name.
     * If the node has no anchor returns null.
     * @return The name of the anchor node.
     */
    public NodeName getAnchorNodeName() {
        return anchorNodeName;
    }
}
