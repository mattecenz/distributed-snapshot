package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Exception.RoutingTableNodeAlreadyPresentException;
import polimi.ds.dsnapshot.Exception.RoutingTableNodeNotPresentException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Optional;

/**
 * Internal routing table of the connection manager.
 * If needed it needs to be accessed atomically
 */
public class RoutingTable implements Serializable {
    /**
     * Internal dictionary of the form: (node name, client socket handler).
     */
    private Dictionary<NodeName, ClientSocketHandler> routingTableFields;

    /**
     * Explicit copy constructor of a routing table
     * @param other other routing table to be copied
     */
    public RoutingTable(RoutingTable other) {
        this.routingTableFields = new Hashtable<>();
        var keys = other.routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            this.routingTableFields.put(key, other.routingTableFields.get(key));
        }
    }

    /**
     * Default constructor with no entries in the table.
     */
    public RoutingTable(){
        LoggerManager.getInstance().mutableInfo("create new RoutingTable", Optional.of(this.getClass().getName()), Optional.of("RoutingTable"));
        this.routingTableFields = new Hashtable<>();
    }

    /**
     * Method to explicitly cleear each entry of the routing table.
     */
    protected void clearTable(){
        LoggerManager.getInstance().mutableInfo("empty routing table completely", Optional.of(this.getClass().getName()), Optional.of("clearTable"));
        Enumeration<NodeName> keys = this.routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            this.routingTableFields.remove(key);
        }
    }

    /**
     * Method to explicitly check if the routing table is empty
     * @return true if it is empty
     */
    protected boolean isEmpty(){
        return this.routingTableFields.isEmpty();
    }

    /**
     * Method to add a new entry in the routing table
     * @param destination name of the destination node
     * @param nextHopConnection handler to call when sending a message to the destination node
     * @throws RoutingTableNodeAlreadyPresentException if the node is already present in the routing table
     */
    protected void addPath(NodeName destination, ClientSocketHandler nextHopConnection) throws RoutingTableNodeAlreadyPresentException {
        if (this.routingTableFields.get(destination) != null) throw new RoutingTableNodeAlreadyPresentException();
        LoggerManager.getInstance().mutableInfo("add new path to the routing table:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("addPath"));

        this.routingTableFields.put(destination,nextHopConnection);

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("addPath"));
    }

    /**
     * Utility method for printing the internal routing table
     */
    protected void printRoutingTable() { //TODO: use log
        System.out.println(this.getRoutingTableString());
    }


    private String getRoutingTableString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Routing Table:\n");
        var keys = routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            sb.append("Node: ")
                    .append(key.getIP()).append(":").append(key.getPort())
                    .append(" -> Handler: ")
                    .append(routingTableFields.get(key))
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Method to update the path to a destination node
     * @param destination destination to be updated
     * @param nextHopConnection new client handler
     * @throws RoutingTableNodeNotPresentException if the node was not present in the routing table
     */
    protected void updatePath(NodeName destination, ClientSocketHandler nextHopConnection) throws RoutingTableNodeNotPresentException {
        if(this.routingTableFields.get(destination)==null) throw new RoutingTableNodeNotPresentException();
        LoggerManager.getInstance().mutableInfo("update existing path in the routing table:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("updatePath"));

        this.routingTableFields.put(destination, nextHopConnection);
    }

    /**
     * Method to explicitly remove the path from a specific destination node
     * @param destination destination to be removed
     * @throws RoutingTableNodeNotPresentException if the node was not present in the routing table
     */
    protected void removePath(NodeName destination) throws RoutingTableNodeNotPresentException {
        if(this.routingTableFields.get(destination) == null) throw new RoutingTableNodeNotPresentException();
        LoggerManager.getInstance().mutableInfo("remove existing path from the routing table:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("removePath"));

        this.routingTableFields.remove(destination);

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("removePath"));
    }

    /**
     * Method to remove all the paths associated to the input handler
     * @param handler client socket handler to be removed
     */
    void removeAllIndirectPath(ClientSocketHandler handler){
        var keys = this.routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            if (this.routingTableFields.get(key).equals(handler)) {
                this.routingTableFields.remove(key);
            }
        }

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("removeAllIndirectPath"));
    }

    /**
     * Return the handler associated to the node name in input
     * @param destination name of the node to be searched in the routing table
     * @return the client socket handler, if found
     * @throws RoutingTableNodeNotPresentException if the node was not found in the routing table
     */
    ClientSocketHandler getNextHop(NodeName destination) throws RoutingTableNodeNotPresentException {
        LoggerManager.getInstance().mutableInfo("requested next hop for:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("getNextHop"));
        ClientSocketHandler nextHop = this.routingTableFields.get(destination);

        if(nextHop == null){
            LoggerManager.getInstance().mutableInfo("next hop is null", Optional.of(this.getClass().getName()), Optional.of("getNextHop"));
            throw new RoutingTableNodeNotPresentException();
        }

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("getNextHop"));
        return nextHop;
    }
}

