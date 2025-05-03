package polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Hashtable;

public class SerializableRoutingTable implements Serializable{

    private Dictionary<NodeName, SerializedSocketHandler> routingTableFields = new Hashtable<>();

    SerializableRoutingTable(Dictionary<NodeName, ClientSocketHandler> routingTableFields) {
        var keys = routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            ClientSocketHandler value = routingTableFields.get(key);
            this.routingTableFields.put(key,new SerializedSocketHandler(value.getRemoteNodeName(), value.isNodeOwner()));
        }
    }

    public Dictionary<NodeName, SerializedSocketHandler> getOldRoutingTableFields() {
        return routingTableFields;
    }
}
