package polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Routing table saved in the snapshot.
 * As the original class cannot be serializable, we need a version where all the important components can be saved to disk.
 */
public class SerializableRoutingTable implements Serializable{

    /**
     * Dictionary containing the tuple : (node name, serializable socket handler).
     */
    private Dictionary<NodeName, SerializedSocketHandler> routingTableFields = new Hashtable<>();

    /**
     * Constructor of the serializable routing table.
     * @param routingTableFields Internal dictionary of the routing table.
     */
    SerializableRoutingTable(Dictionary<NodeName, ClientSocketHandler> routingTableFields) {
        var keys = routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            ClientSocketHandler value = routingTableFields.get(key);
            this.routingTableFields.put(key,new SerializedSocketHandler(value.getRemoteNodeName(), value.isNodeOwner()));
        }
    }

    /**
     * Getter of the internal dictionary.
     * @return The internal dictionary containing the serialized routing table.
     */
    public Dictionary<NodeName, SerializedSocketHandler> getOldRoutingTableFields() {
        return routingTableFields;
    }
}
