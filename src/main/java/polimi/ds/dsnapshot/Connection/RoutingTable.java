package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.NetNode;
import polimi.ds.dsnapshot.Exception.RoutingTableException;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.PrimitiveIterator;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;

public class RoutingTable {
    private Dictionary<NetNode, ClientSocketHandler> routingTableFields;

    protected RoutingTable(){
        routingTableFields = new java.util.Hashtable<>();
    }

    protected void clearTable(){
        routingTableFields = new java.util.Hashtable<>();
    }

    protected boolean isEmpty(){
        return routingTableFields.isEmpty();
    }

    protected void addPath(NetNode destination, ClientSocketHandler nextHopConnection) throws RoutingTableException {
        if (routingTableFields.get(destination) != null) throw new RoutingTableException("destination already in the table");

        routingTableFields.put(destination,nextHopConnection);
    }

    protected void updatePath(NetNode destination, ClientSocketHandler nextHopConnection) throws RoutingTableException {
        if(routingTableFields.get(destination) == null) throw new RoutingTableException("destination isn't present the table");

        routingTableFields.put(destination, nextHopConnection);
    }

    protected void removePath(NetNode destination) throws RoutingTableException {
        if(routingTableFields.get(destination) == null)throw new RoutingTableException("destination isn't present the table");

        routingTableFields.remove(destination);
    }

    protected void removeAllIndirectPath(ClientSocketHandler handler){
        var keys = routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NetNode key = keys.nextElement();
            if (routingTableFields.get(key).equals(handler)) {
                routingTableFields.remove(key);
            }
        }
    }

    protected ClientSocketHandler getNextHop(NetNode destination) throws RoutingTableException {
        ClientSocketHandler nextHop = routingTableFields.get(destination);

        if(nextHop == null) throw new RoutingTableException("destination isn't present the table");
        return nextHop;
    }
}

