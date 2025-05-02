package polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.ConnectionManager;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.SnapshotSerializable;
import polimi.ds.dsnapshot.Exception.RoutingTable.RoutingTableNodeAlreadyPresentException;
import polimi.ds.dsnapshot.Exception.RoutingTable.RoutingTableNodeNotPresentException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;

/**
 * Internal routing table of the connection manager.
 * If needed it needs to be accessed atomically
 */
public class RoutingTable implements SnapshotSerializable {
    /**
     * Internal dictionary of the form: (node name, client socket handler).
     */
    private final Dictionary<NodeName, ClientSocketHandler> routingTableFields;

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
     * Method to explicitly clear each entry of the routing table.
     * It is an atomic operation
     */
    public synchronized void clearTable(){
        LoggerManager.getInstance().mutableInfo("clearing routing table", Optional.of(this.getClass().getName()), Optional.of("clearTable"));
        Enumeration<NodeName> keys = this.routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            this.routingTableFields.remove(key);
        }
    }

    /**
     * Method to explicitly check if the routing table is empty.
     * It is an atomic operation
     * @return true if it is empty
     */
    public synchronized boolean isEmpty(){
        return this.routingTableFields.isEmpty();
    }

    /**
     * Method to add a new entry in the routing table.
     * It is an atomic operation
     * @param destination name of the destination node
     * @param nextHopConnection handler to call when sending a message to the destination node
     * @throws RoutingTableNodeAlreadyPresentException if the node is already present in the routing table
     */
    public synchronized void addPath(NodeName destination, ClientSocketHandler nextHopConnection) throws RoutingTableNodeAlreadyPresentException {
        if (this.routingTableFields.get(destination) != null) throw new RoutingTableNodeAlreadyPresentException();
        LoggerManager.getInstance().mutableInfo("add new path to the routing table:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("addPath"));

        this.routingTableFields.put(destination,nextHopConnection);

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("addPath"));
    }

    /**
     * Utility method for printing the internal routing table.
     * It is an atomic operation
     */
    public synchronized void printRoutingTable() { //TODO: use log
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
     * Method to update the path to a destination node.
     * It is an atomic operation
     * @param destination destination to be updated
     * @param nextHopConnection new client handler
     * @throws RoutingTableNodeNotPresentException if the node was not present in the routing table
     */
    public synchronized void updatePath(NodeName destination, ClientSocketHandler nextHopConnection) throws RoutingTableNodeNotPresentException {
        if(this.routingTableFields.get(destination)==null) throw new RoutingTableNodeNotPresentException();
        LoggerManager.getInstance().mutableInfo("update existing path in the routing table:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("updatePath"));

        this.routingTableFields.put(destination, nextHopConnection);
    }

    /**
     * Method to explicitly remove the path from a specific destination node.
     * It is an atomic operation
     * @param destination destination to be removed
     * @throws RoutingTableNodeNotPresentException if the node was not present in the routing table
     */
    public synchronized void removePath(NodeName destination) throws RoutingTableNodeNotPresentException {
        if(this.routingTableFields.get(destination) == null) throw new RoutingTableNodeNotPresentException();
        LoggerManager.getInstance().mutableInfo("remove existing path from the routing table:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("removePath"));

        this.routingTableFields.remove(destination);

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("removePath"));
    }

    /**
     * Method to remove all the paths associated to the input handler.
     * It is an atomic operation
     * @param handler client socket handler to be removed
     */
    public void removeAllIndirectPath(ClientSocketHandler handler){
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
     * Return the handler associated to the node name in input.
     * It is an atomic operation
     * @param destination name of the node to be searched in the routing table
     * @return the client socket handler, if found
     * @throws RoutingTableNodeNotPresentException if the node was not found in the routing table
     */
    public synchronized ClientSocketHandler getNextHop(NodeName destination) throws RoutingTableNodeNotPresentException {
        LoggerManager.getInstance().mutableInfo("requested next hop for:" + destination.getIP() + ":" + destination.getPort(), Optional.of(this.getClass().getName()), Optional.of("getNextHop"));
        ClientSocketHandler nextHop = this.routingTableFields.get(destination);

        if(nextHop == null){
            LoggerManager.getInstance().mutableInfo("next hop is null", Optional.of(this.getClass().getName()), Optional.of("getNextHop"));
            throw new RoutingTableNodeNotPresentException();
        }

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("getNextHop"));
        return nextHop;
    }

    @Override
    public synchronized Serializable toSerialize(){
        return new SerializableRoutingTable(routingTableFields);
    }

    @Override
    public synchronized boolean serializedValidation(Serializable serializable){
        SerializableRoutingTable serializableRoutingTable = (SerializableRoutingTable) serializable;
        Dictionary<NodeName, NodeName> oldRoutingTableFieldsDict = serializableRoutingTable.getOldRoutingTableFields();
        if (oldRoutingTableFieldsDict == null && !routingTableFields.isEmpty()) return false;
        else if (oldRoutingTableFieldsDict == null && routingTableFields.isEmpty()) return true;

        //check if the serializableRoutingTable is still valid for the current node
        // => if all direct connection in serializableRoutingTable are still present in the current routing table

        //verify that all the direct connection are still present in the new routing table
        Enumeration<NodeName> keys = oldRoutingTableFieldsDict.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            NodeName value = oldRoutingTableFieldsDict.get(key);
            if(key.equals(value)) {
                if(routingTableFields.get(key) == null || !value.equals(routingTableFields.get(key).getRemoteNodeName())) return false;
            }
        }

        //verify that there are no new connection in the current routing table
        keys = routingTableFields.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            NodeName value = routingTableFields.get(key).getRemoteNodeName();

            if(key.equals(value)) {
                if (oldRoutingTableFieldsDict.get(key) == null || !value.equals(oldRoutingTableFieldsDict.get(key))) return false;
            }
        }

        return true;
    }

    public synchronized List<ClientSocketHandler> fromSerialize(SerializableRoutingTable serializableRoutingTable, ConnectionManager manager){
        Dictionary<NodeName, NodeName> oldRoutingTableFieldsDict = serializableRoutingTable.getOldRoutingTableFields();
        removeNonDirectConnections(oldRoutingTableFieldsDict);

        List<ClientSocketHandler> newConnections = new ArrayList<>();
        try {
            newConnections = this.addNewEntries(oldRoutingTableFieldsDict, manager);
        }catch (IOException e){
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IOException", e);
            return newConnections;
            //TODO: decide
        }

        LoggerManager.getInstance().mutableInfo(this.getRoutingTableString(), Optional.of(this.getClass().getName()), Optional.of("fromSerialize"));
        return newConnections;
    }

    private synchronized void removeNonDirectConnections(Dictionary<NodeName, NodeName> oldRoutingTableFieldsDict){
        Enumeration<NodeName> keys = routingTableFields.keys();

        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            NodeName value = routingTableFields.get(key).getRemoteNodeName();

            if(oldRoutingTableFieldsDict.get(key) == null || !value.equals(oldRoutingTableFieldsDict.get(key))){
                routingTableFields.remove(key);
            }
        }
    }

    private synchronized List<ClientSocketHandler> addNewEntries(Dictionary<NodeName, NodeName> oldRoutingTableFieldsDict, ConnectionManager manager) throws IOException {
        Enumeration<NodeName> keys = oldRoutingTableFieldsDict.keys();
        List<ClientSocketHandler> newConnections = new ArrayList<>();

        //direct connection
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            NodeName value = oldRoutingTableFieldsDict.get(key);
            if (key.equals(value) && routingTableFields.get(key) == null) {
                newConnections.add(socketOpen(key, manager));
            } else if (key.equals(value) && !routingTableFields.get(key).getRemoteNodeName().equals(key)) {
                routingTableFields.remove(key);
                newConnections.add(socketOpen(key, manager));
            }
        }

        //non direct connection
        keys = oldRoutingTableFieldsDict.keys();
        while (keys.hasMoreElements()) {
            NodeName key = keys.nextElement();
            NodeName value = oldRoutingTableFieldsDict.get(key);
            if(!key.equals(value)  && routingTableFields.get(key) == null) {
                routingTableFields.put(key,routingTableFields.get(value));
            }
        }

        return newConnections;
    }

    private ClientSocketHandler socketOpen(NodeName destination, ConnectionManager manager) throws IOException {
        Socket socket = new Socket(destination.getIP(),destination.getPort());
        ClientSocketHandler joinerHandler = new ClientSocketHandler(socket, destination,manager);
        ThreadPool.submit(joinerHandler);

        routingTableFields.put(destination, joinerHandler);

        return joinerHandler;
    }

}

