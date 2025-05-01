package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.*;
import polimi.ds.dsnapshot.Connection.Messages.Exit.AdoptionRequestMsg;
import polimi.ds.dsnapshot.Connection.Messages.Exit.ExitMsg;
import polimi.ds.dsnapshot.Connection.Messages.Exit.ExitNotify;
import polimi.ds.dsnapshot.Connection.Messages.Join.DirectConnectionMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinForwardMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinMsg;
import polimi.ds.dsnapshot.Connection.Messages.Snapshot.RestoreSnapshotRequest;
import polimi.ds.dsnapshot.Connection.Messages.Snapshot.RestoreSnapshotRequestAgreementResult;
import polimi.ds.dsnapshot.Connection.Messages.Snapshot.RestoreSnapshotResponse;
import polimi.ds.dsnapshot.Connection.Messages.Snapshot.TokenMessage;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.RoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.SPT.SpanningTree;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Api.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Exception.ExportedException.SnapshotRestoreLocalException;
import polimi.ds.dsnapshot.Exception.ExportedException.SnapshotRestoreRemoteException;
import polimi.ds.dsnapshot.Snapshot.SnapshotIdentifier;
import polimi.ds.dsnapshot.Snapshot.SnapshotManager;
import polimi.ds.dsnapshot.Snapshot.SnapshotPendingRequestManager;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

/**
 * The connection manager consists of a TCP server socket who can receive connections.
 * When a new connection is received a channel is opened with which we can exchange messages.
 */
public class ConnectionManager {

    /**
     * List of active connections which do not have yet a name
     */
    private final List<UnNamedSocketHandler> unNamedHandlerList;
    /**
     * List of all the active connections in the network
     */
    private final List<ClientSocketHandler> handlerList;
    /**
     * Routing table of the application.
     * It is an atomic reference since all the operations in it must be run atomically to avoid inconsistencies
     */
    private final RoutingTable routingTable = new RoutingTable();
    private final SpanningTree spt = new SpanningTree();
    private final SnapshotManager snapshotManager = new SnapshotManager(this);
    private SnapshotPendingRequestManager snapshotPendingRequestManager = null;
    /**
     * Reference to the handler of the acks
     */
    private final AckHandler ackHandler;
    /**
     * Name of this connection manager.
     * For the moment we can assume it is immutable
     */
    private final NodeName name;
    private Event toForwardEvent;

    double directConnectionProbability = Config.getDouble("network.directConnectionProbability");

    /**
     * Constructor of the connection manager
     */
    public ConnectionManager(int port){
        this.unNamedHandlerList = new ArrayList<>();
        this.handlerList = new ArrayList<>();
        this.ackHandler = new AckHandler();

        // Default value if something goes wrong
        String thisIP = "127.0.0.1";
        try{
            InetAddress localHost = InetAddress.getLocalHost();
            // Get the IP address as a string
            thisIP = localHost.getHostAddress();
        }
        catch(UnknownHostException e){
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Host not connected to network, cannot do anything:", e);
        }
        this.name = new NodeName(thisIP, port);
        try {
            toForwardEvent = EventsBroker.createEventChannel("toForward");
        } catch (EventException e) {
            LoggerManager.getInstance().mutableInfo("to forward event already exist", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
            try {
                toForwardEvent = EventsBroker.getEventChannel("toForward");
            } catch (EventException ex) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "Failed to get event channel", ex);
                return;
                //TODO: decide
            }
        }
        toForwardEvent.subscribe(this::forwardMessage);

        LoggerManager.getInstance().mutableInfo("ConnectionManager created successfully. My name is: "+thisIP+":"+port , Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
    }

    public NodeName getName() {
        return name;
    }

    public void start(){
        LoggerManager.getInstance().mutableInfo("Preparing the thread...", Optional.of(this.getClass().getName()), Optional.of("start"));

        // This start has to launch another thread.

        Thread t = new Thread(()->{

            try(ServerSocket serverSocket = new ServerSocket(this.name.getPort())){
                LoggerManager.getInstance().mutableInfo("Created listening socket on port "+this.name.getPort()+" ...", Optional.of(this.getClass().getName()), Optional.of("start"));

                while(true){
                    LoggerManager.getInstance().mutableInfo("Waiting for connection...", Optional.of(this.getClass().getName()), Optional.of("start"));
                    Socket socket = serverSocket.accept();
                    LoggerManager.getInstance().mutableInfo("Accepted connection from " + socket.getRemoteSocketAddress()+" ...", Optional.of(this.getClass().getName()), Optional.of("start"));
                    // When you receive a new connection add it to the list of unnamed connections
                    UnNamedSocketHandler unhandler = new UnNamedSocketHandler(socket, this);
                    synchronized(this.unNamedHandlerList){
                        this.unNamedHandlerList.add(unhandler);
                    }
                    // Submit the thread which will wait for the join message
                    ThreadPool.submit(unhandler);
                    LoggerManager.getInstance().mutableInfo("Connection submitted to executor...", Optional.of(this.getClass().getName()), Optional.of("start"));
                }

            }catch (IOException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                // TODO: what to do ?
            }
            // Here the serverSocket is closed
            LoggerManager.getInstance().mutableInfo("Shutting down...", Optional.of(this.getClass().getName()), Optional.of("start"));
        });

        LoggerManager.getInstance().mutableInfo("Launching the thread...", Optional.of(this.getClass().getName()), Optional.of("start"));

        t.start();
    }

    // TODO: maybe its better if the method is private (called by a generic sendMessage that works as interface)
    // TODO: refactor well to work with exceptions
    // TODO: discuss a bit if every message needs the destination ip:port
    // TODO: there is a problem, the MessageAck is a different class than the Message
    boolean sendMessageSynchronized(Message m, String ip, int port){
        LoggerManager.getInstance().mutableInfo("Sending a message"+ m.getClass().getName() +" to "+ip+":"+port+"...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));

        NodeName destNode = new NodeName(ip, port);

        try {
            LoggerManager.getInstance().mutableInfo("Checking the routing table for the next hop...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            ClientSocketHandler handler = this.routingTable.getNextHop(destNode);

            return this.sendMessageSynchronized(m,handler);
        } catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "RoutingTableNodeNotPresentException", e);
            return false;
        } catch (ConnectionException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "ConnectionException", e);
            //todo: ack not received
            return false;
        }
    }

    protected boolean sendMessageSynchronized(Message m, ClientSocketHandler handler) throws ConnectionException{
        LoggerManager.getInstance().mutableInfo("Sending a message: "+ m.getClass().getName() +" to "+handler.getRemoteNodeName().getIP()+":"+handler.getRemoteNodeName().getPort()+"...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
        LoggerManager.getInstance().mutableInfo("Preparing for receiving an ack...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
        int seqn = m.getSequenceNumber();
        // Insert in the handler the number and the thread to wait
        Object lock=new Object();
        this.ackHandler.insertAckId(seqn, lock);

        LoggerManager.getInstance().mutableInfo("Sending the message ...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
        boolean b = handler.sendMessage(m);

        if(!b) {
            LoggerManager.getInstance().mutableInfo("Something went wrong while sending the message...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return false;
        }

        LoggerManager.getInstance().mutableInfo("Sent, now waiting for ack...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));

        try {
            // Wait for a timeout, if ack has been received then all good, else something bad happened.
            synchronized (lock) {
                lock.wait(Config.getInt("network.ackTimeout"));
            }
            // Once I have finished I have two possibilities. Either the ack has been removed from the list or not
            // If it has been removed then an exception is thrown.
            this.ackHandler.removeAckId(seqn);
        }
        catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
            return false;
        }
        catch (AckHandlerAlreadyRemovedException e) {
            // If a runtime exception is thrown it means that the ack has been removed, so it has been received.
            LoggerManager.getInstance().mutableInfo("Ack received, can resume operations...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return true;
        }
        // TODO: handle error of ack
        // If no exception is thrown then it means that
        LoggerManager.instanceGetLogger().log(Level.WARNING, "Timeout reached waiting for ack.");
        throw new ConnectionException("[ConnectionManager] Timeout reached waiting for ack");
    }

    /**
     * Processes a direct connection message received from another node
     * @param nodeName Name of the node to be added to the table
     * @param handler handler of the socket
     * @throws RoutingTableNodeAlreadyPresentException if the ip address is already in the routing table
     */
    private void addNewRoutingTableEntry(NodeName nodeName, ClientSocketHandler handler) throws RoutingTableNodeAlreadyPresentException {
        this.routingTable.addPath(nodeName,handler);
    }

    // <editor-fold desc="Join procedure">
    /**
     * Establishes a connection to an anchor node in the network by creating a socket connection.
     * Sends a `JoinMsg` message to the specified node in order to initiate the join process.
     * @param anchorName name of the anchor node to connect to
     * @throws IOException if an I/O error occurs during socket connection or communication
     */
    public void joinNetwork(NodeName anchorName) throws IOException {
        JoinMsg msg = new JoinMsg(this.name);
        joinNetwork(anchorName,msg);
    }

    private void joinNetwork(NodeName anchorName, JoinMsg joinMsg) throws IOException {
        Socket socket = new Socket(anchorName.getIP(),anchorName.getPort());
        //create socket for the anchor node, add to direct connection list and save as anchor node
        ClientSocketHandler handler = new ClientSocketHandler(socket, anchorName,this);
        ThreadPool.submit(handler);
        //send join msg to anchor node & wait for ack

        try {
            LoggerManager.getInstance().mutableInfo("Joining the network to anchor "+anchorName.getIP()+":"+anchorName.getPort(), Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));

            // TODO: check potential error here
            boolean ret=this.sendMessageSynchronized(joinMsg,handler);

            if(!ret) {
                LoggerManager.getInstance().mutableInfo("Something went wrong. Maybe it was a lock problem, so retry...", Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));
                return;
            }

            LoggerManager.getInstance().mutableInfo("Ack from anchor received!", Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));

            // If everything went well continue...

            // Add it as a parent in the spt
            this.spt.setAnchorNodeHandler(handler);
            // Add it to the active list of handlers
            synchronized(this.handlerList){
                this.handlerList.add(handler);
            }
            // Add it to the routing table
            this.routingTable.addPath(anchorName, handler);
            // Start the ping pong with the handler
            handler.startPingPong(true);
        } catch (ConnectionException e) {
            //todo: ack not received
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Error waiting for ack:", e);
        } catch (RoutingTableNodeAlreadyPresentException e) {
            // Should be impossible to reach this exception
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Node already present in routing table: ", e);
        }
    }

    /**
    * Handles a join request received from another node in the network.
    *
    * @param joinMsg Message received
    * @param unnamedHandler the {@link UnNamedSocketHandler} managing the client communication
    *                for the incoming connection.
    */
    synchronized void receiveNewJoinMessage(JoinMsg joinMsg, UnNamedSocketHandler unnamedHandler) {
        try {
            ClientSocketHandler handler = receiveAdoptionOrJoinRequest(joinMsg, unnamedHandler);
            sendJoinForwardMsg(joinMsg,handler);
        } catch (RoutingTableNodeAlreadyPresentException e) {//TODO: decide
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Node already exists in routing table: ", e);
            return;
        }
    }

    synchronized ClientSocketHandler receiveAdoptionOrJoinRequest(JoinMsg joinMsg, UnNamedSocketHandler unnamedHandler) throws RoutingTableNodeAlreadyPresentException {
        LoggerManager.getInstance().mutableInfo("Join request received from node "+joinMsg.getJoinerName().getIP()+":"+joinMsg.getJoinerName().getPort(), Optional.of(this.getClass().getName()), Optional.of("receiveAdoptionOrJoinRequest"));
        // Create the new handler
        ClientSocketHandler handler = new ClientSocketHandler(unnamedHandler, joinMsg.getJoinerName(), this);
        // Add it in the current handler list
        synchronized (this.handlerList) {
            this.handlerList.add(handler);
        }
        // Remove the unnamed handler from the list. The garbage collector will do its job later
        synchronized (this.unNamedHandlerList){
            this.unNamedHandlerList.remove(unnamedHandler);
        }
        // Add a new routing table entry
        this.addNewRoutingTableEntry(handler.getRemoteNodeName(), handler);
        // Since it is a new direct connection I need to add it to the spt
        // this.spt.get().getChildren().add(handler);
        // Submit the new handler to the thread pool
        ThreadPool.submit(handler);

        // Since the join is a synchronous process we need to send back the ack
        MessageAck msgAck = new MessageAck(joinMsg.getSequenceNumber());
        LoggerManager.getInstance().mutableInfo("Join request ack back", Optional.of(this.getClass().getName()), Optional.of("receiveAdoptionOrJoinRequest"));
        boolean ret=false;
        // TODO: refactor a bit with exceptions
        while(!ret){
            ret=handler.sendMessage(msgAck);
            if(!ret) LoggerManager.getInstance().mutableInfo("Something went wrong. Maybe it was a lock problem, so retry...", Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));
        }
        return handler;
    }

    private void sendJoinForwardMsg(JoinMsg joinMsg, ClientSocketHandler handler) {
        //forward join notify to active neighbours
        JoinForwardMsg m = new JoinForwardMsg(joinMsg.getJoinerName());

        LoggerManager.getInstance().mutableInfo("Forwarding info to the other nodes.", Optional.of(this.getClass().getName()), Optional.of("receiveNewJoinMessage"));

        for(ClientSocketHandler h : this.handlerList){
            if(h!=handler) h.sendMessage(m);
        }
    }

    /**
     * Handles a direct connection request received from another node in the network.
     *
     * @param directConnectionMsg Message received
     * @param unnamedHandler the {@link UnNamedSocketHandler} managing the client communication
     *                for the incoming connection.
     */
    synchronized void receiveNewDirectConnectionMessage(DirectConnectionMsg directConnectionMsg, UnNamedSocketHandler unnamedHandler) {
        // TODO: refactor duplicate code with above function
        try {
            LoggerManager.getInstance().mutableInfo("Direct connection request received from node: "+directConnectionMsg.getJoinerName().getIP()+":"+directConnectionMsg.getJoinerName().getPort(), Optional.of(this.getClass().getName()), Optional.of("receiveNewDirectConnectionMessage"));
            //create socket for the anchor node, add to direct connection list and save as anchor node
            ClientSocketHandler handler = new ClientSocketHandler(unnamedHandler, directConnectionMsg.getJoinerName(),this);
            ThreadPool.submit(handler);
            // Remove the unnamed handler from the list. The garbage collector will do its job later
            synchronized (this.unNamedHandlerList){
                this.unNamedHandlerList.remove(unnamedHandler);
            }
            // Add a new routing table entry
            this.addNewRoutingTableEntry(handler.getRemoteNodeName(), handler);
            // Since it is not a direct connection it does not need to be added to the spt
            // Submit the new handler to the thread pool

            // Since the direct connection is not synchronous we do not need to send back an ack

            // And no forwarding

        } catch (RoutingTableNodeAlreadyPresentException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here, a node already in the routing table asked to connect", e);
            //TODO manage: if I receive a join from a node already in the routing table (wtf)
            return;
        }
    }

    /**
     * Handles a forwarded join request in the network.
     * @param msg the {@link JoinForwardMsg} containing details about the forwarder and the joiner.
     * @param handler the {@link ClientSocketHandler} managing the client communication
     */
    private void receiveJoinForward(JoinForwardMsg msg, ClientSocketHandler handler) throws IOException {
        double randomValue = ThreadLocalRandom.current().nextDouble();
        LoggerManager.getInstance().mutableInfo("receive join forward from node: " + handler.getRemoteNodeName().getIP() + ":" +handler.getRemoteNodeName().getPort() + " for joiner node: " + msg.getJoinerName().getIP() +":"+ msg.getJoinerName().getPort(), Optional.of(this.getClass().getName()), Optional.of("receiveJoinForward"));
        try {
            if(randomValue < this.directConnectionProbability){
                LoggerManager.getInstance().mutableInfo("probability win, create direct connection", Optional.of(this.getClass().getName()),Optional.of("receiveJoinForward"));
                // Open a new socket
                Socket socket = new Socket(msg.getJoinerName().getIP(),msg.getJoinerName().getPort());
                // Create the new handler
                ClientSocketHandler joinerHandler = new ClientSocketHandler(socket, msg.getJoinerName(),this);
                ThreadPool.submit(joinerHandler);
                // Add it in the current handler list
                synchronized (this.handlerList) {
                    this.handlerList.add(joinerHandler);
                }
                //send to joiner a message to create a direct connection
                boolean ret=false;
                while(!ret) {
                    ret = joinerHandler.sendMessage(new DirectConnectionMsg(this.name));
                }
                //add node in routing table
                this.addNewRoutingTableEntry(msg.getJoinerName(), joinerHandler);
                // Do not to spt as it is not a direct connection
            }else {
                //creating undirected path to the joiner node with the anchor node
                this.routingTable.addPath(msg.getJoinerName(),handler);
            }
        } catch (RoutingTableNodeAlreadyPresentException e) {
            // Not much we can do
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here, a node already in the routing table asked to connect", e);
        }
    }
    // </editor-fold>

    // <editor-fold desc="Exit procedure">
    public synchronized void exitNetwork() throws IOException{
        LoggerManager.getInstance().mutableInfo("Exit procedure started", Optional.of(this.getClass().getName()), Optional.of("exitNetwork"));

        //stop children ping pong
        for(ClientSocketHandler h : this.spt.getChildren()){
           h.stopPingPong();
        }
        //reassign all child to the current anchor node of the exiting node
        ClientSocketHandler handler;
        try{
            handler = this.spt.getAnchorNodeHandler();
            handler.stopPingPong(); //stop anchor ping pong
        } catch (SpanningTreeNoAnchorNodeException e) {
            if(this.spt.getChildren().isEmpty()){
                LoggerManager.instanceGetLogger().log(Level.WARNING, "Trying to exit the network with no parent and no children. Do nothing");
                return;
            }
            // Assign the new owner of the network the first child.
            handler = this.spt.getChildren().getFirst();
        }

        //send exit message to all child

        ExitMsg m = new ExitMsg(handler.getRemoteNodeName());
        this.forwardMessageAlongSPT(m, Optional.empty());
        LoggerManager.getInstance().mutableInfo("send exit msg on spt", Optional.of(this.getClass().getName()), Optional.of("exitNetwork"));

        //clear handler list
        synchronized (this.unNamedHandlerList){
            this.unNamedHandlerList.clear();
        }
        LoggerManager.getInstance().mutableInfo("clear unnamed list during exit", Optional.of(this.getClass().getName()), Optional.of("exitNetwork"));

        synchronized (this.handlerList){
            this.handlerList.clear();
        }
        LoggerManager.getInstance().mutableInfo("clear handler list during exit", Optional.of(this.getClass().getName()), Optional.of("exitNetwork"));

        //clear routing table
        this.routingTable.clearTable();
        LoggerManager.getInstance().mutableInfo("clear routing table during exit", Optional.of(this.getClass().getName()), Optional.of("exitNetwork"));
    }

    private void receiveExit(ExitMsg msg, ClientSocketHandler handler) throws IOException {
        LoggerManager.getInstance().mutableInfo("receive exit from: " +handler.getRemoteNodeName().getIP()+":"+handler.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("receiveExit"));
        try{
            handler.stopPingPong();
            this.routingTable.removePath(handler.getRemoteNodeName());
        }catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here, a node not present in the routing table send an exit", e);
            //TODO if ip not in routing table
        }

        this.routingTable.removeAllIndirectPath(handler);
        this.handlerList.remove(handler);
        handler.close();

        try {
            ClientSocketHandler anchorNodeHandler = this.spt.getAnchorNodeHandler();

            if(handler == anchorNodeHandler){
                LoggerManager.getInstance().mutableInfo("exit received from anchor", Optional.of(this.getClass().getName()), Optional.of("receiveExit"));
                // Remove anchor node for the moment

                    this.spt.removeAnchorNodeHandler();

                this.newAnchorNode(msg);
            }
        }catch (SpanningTreeNoAnchorNodeException e) {
            LoggerManager.getInstance().mutableInfo("exit received from the network first node", Optional.of(this.getClass().getName()), Optional.of("receiveExit"));
            //TODO if no anchor node exist -> if leader
        }

        List<ClientSocketHandler> children = this.spt.getChildren();
        if(children.contains(handler)){
            this.spt.removeChild(handler);
        }

        this.sendExitNotify(handler.getRemoteNodeName(), Optional.empty());
        JavaDistributedSnapshot.getInstance().applicationExitNotify(handler.getRemoteNodeName());

    }
    /**
     * Handles the assignment of a new anchor node when the current anchor node exits the network.
     * This method determines whether a path to the new anchor exists in the routing table
     * and establishes a direct connection if necessary.
     * @param msg     the {@link ExitMsg} containing the IP and port of the new anchor node.
     */
    private void newAnchorNode(ExitMsg msg) throws IOException {
        LoggerManager.getInstance().mutableInfo("trying to assign new anchor...", Optional.of(this.getClass().getName()), Optional.of("newAnchorNode"));

        // Corner case: if the new node to connect to is myself do not do nothing. As I will become the new "owner of the network

        if(msg.getNewAnchorName().equals(this.name)){
            LoggerManager.getInstance().mutableInfo("I am the new owner of the network, do not need to connect to an handler.", Optional.of(this.getClass().getName()), Optional.of("newAnchorNode"));
            return;
        }

        ClientSocketHandler newAnchorNextHop;
        try {
            // Attempt to fetch the next hop in the routing table for the new anchor node.
            newAnchorNextHop = this.routingTable.getNextHop(msg.getNewAnchorName());
            LoggerManager.getInstance().mutableInfo("the new anchor is a known node", Optional.of(this.getClass().getName()), Optional.of("newAnchorNode"));
        } catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.getInstance().mutableInfo("the new anchor is not a known node", Optional.of(this.getClass().getName()), Optional.of("newAnchorNode"));
            // No path to reach the new anchor node, establish a direct connection.
            newAnchorNodeEstablishDirectConnection(msg.getNewAnchorName());
            return;
        }

        // Check if there is already a direct connection with the new anchor node.
        if (newAnchorNextHop.getRemoteNodeName().equals(msg.getNewAnchorName())) {
            LoggerManager.getInstance().mutableInfo("a direct connection with the new anchor is available", Optional.of(this.getClass().getName()), Optional.of("newAnchorNode"));
            newAnchorNextHop.startPingPong(true);
            //set new anchor node
            this.spt.setAnchorNodeHandler(newAnchorNextHop);
            return;
        }
        LoggerManager.getInstance().mutableInfo("a direct connection with the new anchor is not available", Optional.of(this.getClass().getName()), Optional.of("newAnchorNode"));
        // No direct connection with the new anchor node; establish one.
        this.newAnchorNodeEstablishDirectConnection(msg.getNewAnchorName());
    }

    private void newAnchorNodeEstablishDirectConnection(NodeName nodeName) {
        AdoptionRequestMsg msg = new AdoptionRequestMsg(this.name);
        ThreadPool.submit(()->{
            try {
                this.joinNetwork(nodeName,msg);
            } catch (IOException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE,"IOException when ensablish connection with new Anchor", e);
            }
        });
    }

    private void sendExitNotify(NodeName nodeName, Optional<ClientSocketHandler> handler){
        LoggerManager.getInstance().mutableInfo("send exit notify", Optional.of(this.getClass().getName()), Optional.of("sendExitNotify"));
        ExitNotify exitNotify = new ExitNotify(nodeName);
        this.forwardMessageAlongSPT(exitNotify, handler);
    }

    private void receiveExitNotify(ExitNotify exitNotify, ClientSocketHandler handler){
            LoggerManager.getInstance().mutableInfo("received exit notify for node: " +exitNotify.getExitName().getIP()+ ":" +exitNotify.getExitName().getPort(), Optional.of(this.getClass().getName()), Optional.of("receiveExitNotify"));
        try {
            this.routingTable.removePath(exitNotify.getExitName());
        } catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.getInstance().mutableInfo("received exit notify for unknown node", Optional.of(this.getClass().getName()), Optional.of("receiveExitNotify"));
        }
        JavaDistributedSnapshot.getInstance().applicationExitNotify(exitNotify.getExitName());
        this.sendExitNotify(exitNotify.getExitName(), Optional.ofNullable(handler));
    }

    // </editor-fold>

    // <editor-fold desc="Snapshot procedure">
    private void forwardToken(TokenMessage tokenMessage){
        for(ClientSocketHandler h : this.handlerList){
            h.sendMessage(tokenMessage);
        }
    }

    public synchronized void startNewSnapshot(){
        //snapshot preparation
        String CHARACTERS = Config.getString("snapshot.codeAdmissibleChars");
        SecureRandom RANDOM = new SecureRandom();
        String snapshotCode= RANDOM.ints(Config.getInt("snapshot.uniqueCodeSize"), 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt)
                .map(String::valueOf)
                .collect(Collectors.joining());
        TokenMessage tokenMessage = new TokenMessage(snapshotCode, name);
        String tokenName = snapshotCode+"-"+name.getIP()+"-"+name.getPort();
        //start snapshot locally
        this.snapshotManager.manageSnapshotToken(tokenName,name);

        //notify the rest of the network
        for(ClientSocketHandler h : this.handlerList){
            h.sendMessage(tokenMessage);
        }
    }
    // </editor-fold>

    // <editor-fold desc="restore Snapshot procedure">
    public void startSnapshotRestoreProcedure(SnapshotIdentifier snapshotIdentifier) throws SnapshotRestoreLocalException, SnapshotRestoreRemoteException {
        LoggerManager.getInstance().mutableInfo("starting snapshot restore procedure (2PC)", Optional.of(this.getClass().getName()), Optional.of("startSnapshotRestoreProcedure"));
        RestoreSnapshotRequest restoreSnapshotRequest = new RestoreSnapshotRequest(snapshotIdentifier);
        if(! snapshotManager.reEnteringNodeValidateSnapshotRequest(snapshotIdentifier)) throw new SnapshotRestoreLocalException("is not possible to restore the snapshot!");

        synchronized (this){
            LoggerManager.getInstance().mutableInfo("set snapshotPendingRequestManager", Optional.of(this.getClass().getName()), Optional.of("startSnapshotRestoreProcedure"));
            snapshotPendingRequestManager =  new SnapshotPendingRequestManager(Optional.empty(), snapshotIdentifier);
            this.fillPendingRequests(Optional.empty());
            forwardMessageAlongSPT(restoreSnapshotRequest, Optional.empty());
        }


        //wait for response
        Object lock = snapshotPendingRequestManager.getSnapshotLock();
        try {
            synchronized(lock){
                lock.wait(Config.getInt("snapshot.snapshotRestore2PCTimeout"));

                if(!snapshotPendingRequestManager.isEmpty(snapshotIdentifier)){
                    LoggerManager.instanceGetLogger().log(Level.WARNING,"snapshot procedure fail due to timeout expiration");
                    throw new SnapshotRestoreRemoteException("is not possible to restore the snapshot!");
                }


            }
            synchronized (this) {
                LoggerManager.getInstance().mutableInfo("reset snapshotPendingRequestManager", Optional.of(this.getClass().getName()), Optional.of("startSnapshotRestoreProcedure"));
                snapshotPendingRequestManager = null;
            }
        } catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
            throw new SnapshotRestoreLocalException("is not possible to restore the snapshot!");
            //TODO: decide
        } catch (SnapshotPendingRequestManagerException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"received inconsistent snapshot response",e);
            throw new SnapshotRestoreLocalException("is not possible to restore the snapshot!");
            //TODO: decide
        }
    }

    private void fillPendingRequests(Optional<NodeName> sender){
        try {
            NodeName anchorNodeName = spt.getAnchorNodeHandler().getRemoteNodeName();
            if(sender.isEmpty() || !anchorNodeName.equals(sender.get()))snapshotPendingRequestManager.addPendingRequest(anchorNodeName);
        } catch (SpanningTreeNoAnchorNodeException e) {
            LoggerManager.getInstance().mutableInfo("starting snapshot from a node without anchor", Optional.of(this.getClass().getName()), Optional.of("startSnapshotRestoreProcedure"));
            //TODO: decide
        }

        for(ClientSocketHandler h : spt.getChildren()){
            NodeName childNodeName = h.getRemoteNodeName();
            if(sender.isEmpty() || !childNodeName.equals(sender.get()))snapshotPendingRequestManager.addPendingRequest(childNodeName);
        }
        LoggerManager.getInstance().mutableInfo("the node is waiting for " + snapshotPendingRequestManager.pendingRequestCount() + " pending request before restoring the snapshot", Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreRequest"));
    }

    private void receiveSnapshotRestoreRequest(RestoreSnapshotRequest restoreSnapshotRequest, ClientSocketHandler sender){
        LoggerManager.getInstance().mutableInfo("the node has received a restore request", Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreRequest"));
        boolean snapshotValid = snapshotManager.validateSnapshotRequest(restoreSnapshotRequest);

        if(!snapshotValid || this.spt.isNodeLeaf()) {
            sender.sendMessage(new RestoreSnapshotResponse(restoreSnapshotRequest, snapshotValid));
            return;
        }
        SnapshotIdentifier snapshotIdentifier = restoreSnapshotRequest.getSnapshotIdentifier();
        synchronized (this){
            LoggerManager.getInstance().mutableInfo("set snapshotPendingRequestManager", Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreRequest"));
            snapshotPendingRequestManager =  new SnapshotPendingRequestManager(Optional.ofNullable(sender), snapshotIdentifier);
            this.fillPendingRequests(Optional.ofNullable(sender.getRemoteNodeName()));

            forwardMessageAlongSPT(restoreSnapshotRequest, Optional.ofNullable(sender));
        }

        //wait for response
        Object lock = snapshotPendingRequestManager.getSnapshotLock();
        try {
            synchronized(lock){
                lock.wait(Config.getInt("snapshot.snapshotRestore2PCTimeout"));

                if(!snapshotPendingRequestManager.isEmpty(snapshotIdentifier)){
                    LoggerManager.instanceGetLogger().log(Level.WARNING,"snapshot procedure fail due to timeout expiration");
                    sender.sendMessage(new RestoreSnapshotResponse(restoreSnapshotRequest, false));
                }
            }
            synchronized (this) {
                LoggerManager.getInstance().mutableInfo("reset snapshotPendingRequestManager", Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreRequest"));
                snapshotPendingRequestManager = null;
            }
        } catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
        } catch (SnapshotPendingRequestManagerException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"received inconsistent snapshot response",e);
            //TODO: decide
        }
    }

    private synchronized void receiveSnapshotRestoreResponse(RestoreSnapshotResponse restoreSnapshotResponse, ClientSocketHandler handler){
        LoggerManager.getInstance().mutableInfo("the node has received a restore response with value: " + restoreSnapshotResponse.isSnapshotValid(), Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreResponse"));
        if(snapshotPendingRequestManager == null) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"No snapshotPendingRequestManager, skipping.");
            return;
        }
        try {
            if(snapshotPendingRequestManager.isNodeSnapshotLeader(restoreSnapshotResponse.getSnapshotIdentifier())){
                this.leaderReceiveSnapshotRestoreResponse(restoreSnapshotResponse,handler);
                return;
            }

            if(!restoreSnapshotResponse.isSnapshotValid()){
                LoggerManager.getInstance().mutableInfo("send back negative response", Optional.of(this.getClass().getName()), Optional.of("leaderReceiveSnapshotRestoreResponse"));
                snapshotPendingRequestManager.getSnapshotRequestSender(restoreSnapshotResponse.getSnapshotIdentifier()).sendMessage(restoreSnapshotResponse);

                Object lock = snapshotPendingRequestManager.getSnapshotLock();
                synchronized(lock){
                    lock.notifyAll();
                }
                return;
            }
            if(snapshotPendingRequestManager.removePendingRequest(handler.getRemoteNodeName(),restoreSnapshotResponse.getSnapshotIdentifier())){
                //all pending request has been received
                //or receive an invalid response => I can forward to the leader without waiting for other requests
                LoggerManager.getInstance().mutableInfo("the last pending request has been response, send back result", Optional.of(this.getClass().getName()), Optional.of("leaderReceiveSnapshotRestoreResponse"));
                snapshotPendingRequestManager.getSnapshotRequestSender(restoreSnapshotResponse.getSnapshotIdentifier()).sendMessage(restoreSnapshotResponse);
            }

        } catch (SnapshotPendingRequestManagerException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"received inconsistent snapshot response",e);
            //TODO: decide
        }
    }

    private synchronized void leaderReceiveSnapshotRestoreResponse(RestoreSnapshotResponse restoreSnapshotResponse, ClientSocketHandler handler) {
        LoggerManager.getInstance().mutableInfo("the snapshot leader has received a restore response with value: " + restoreSnapshotResponse.isSnapshotValid(), Optional.of(this.getClass().getName()), Optional.of("leaderReceiveSnapshotRestoreResponse"));
        try {
            RestoreSnapshotRequestAgreementResult result = new RestoreSnapshotRequestAgreementResult(restoreSnapshotResponse);

            if (!restoreSnapshotResponse.isSnapshotValid()){

                forwardMessageAlongSPT(result, Optional.empty());

                Object lock = snapshotPendingRequestManager.getSnapshotLock();
                synchronized(lock){
                    lock.notifyAll();
                }
                tryToRestoreSnapshot(restoreSnapshotResponse.getSnapshotIdentifier(),restoreSnapshotResponse.isSnapshotValid());
                return;
            }

            if(snapshotPendingRequestManager.removePendingRequest(handler.getRemoteNodeName(),restoreSnapshotResponse.getSnapshotIdentifier())){
                forwardMessageAlongSPT(result, Optional.empty());
                tryToRestoreSnapshot(restoreSnapshotResponse.getSnapshotIdentifier(),restoreSnapshotResponse.isSnapshotValid());
            }

        } catch (SnapshotPendingRequestManagerException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"received inconsistent snapshot response",e);
            //TODO: decide
        }

    }

    private synchronized void receiveAgreementResult(RestoreSnapshotRequestAgreementResult agreementResult, ClientSocketHandler handler){
        forwardMessageAlongSPT(agreementResult, Optional.ofNullable(handler));
        tryToRestoreSnapshot(agreementResult.getSnapshotIdentifier(), agreementResult.getAgreementResult());

        //todo: we need to notify? (in case of negative response)
    }

    private synchronized void tryToRestoreSnapshot(SnapshotIdentifier snapshotIdentifier, boolean result){
        LoggerManager.getInstance().mutableInfo("try to restore the snapshot after 2PC", Optional.of(this.getClass().getName()), Optional.of("tryToRestoreSnapshot"));
        if(result) {
            LoggerManager.getInstance().mutableInfo("restoring...", Optional.of(this.getClass().getName()), Optional.of("tryToRestoreSnapshot"));
            try {
                //restore network
                List<ClientSocketHandler> newConnections = snapshotManager.restoreSnapshotRoutingTable(snapshotIdentifier);
                for(ClientSocketHandler clientSocketHandler : newConnections){
                    clientSocketHandler.sendMessage(new DirectConnectionMsg(this.name));
                }
                //add to handlerList
                synchronized (this.handlerList) {
                    this.handlerList.addAll(newConnections);
                }
                LoggerManager.getInstance().mutableInfo("routing table restored!", Optional.of(this.getClass().getName()), Optional.of("tryToRestoreSnapshot"));
                //restore app
                snapshotManager.restoreSnapshot(snapshotIdentifier);
            } catch (EventException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE,"restoreSnapshot failed",e);
                //todo decide
            }
            LoggerManager.getInstance().mutableInfo("app state restored!", Optional.of(this.getClass().getName()), Optional.of("tryToRestoreSnapshot"));
            LoggerManager.getInstance().mutableInfo("messages restored!", Optional.of(this.getClass().getName()), Optional.of("tryToRestoreSnapshot"));
            LoggerManager.getInstance().mutableInfo("snapshot completely restored!", Optional.of(this.getClass().getName()), Optional.of("tryToRestoreSnapshot"));
        }
        else{
            LoggerManager.getInstance().mutableInfo("negative response cannot restore the snapshot, abort procedure!", Optional.of(this.getClass().getName()), Optional.of("tryToRestoreSnapshot"));
            snapshotManager.removeSnapshotRequest(snapshotIdentifier);
        }
    }

    // </editor-fold>

    /**
     * Method used for forwarding a message not contained in the routing table.
     * It sends the message along all paths of the spanning tree saved
     * @param msg message to forward
     * @param receivedHandler handler from which the message has been received
     * @return true if everything went well.
     */
    private boolean forwardMessageAlongSPT(Message msg, Optional<ClientSocketHandler> receivedHandler){
        boolean ok = true;

        // I can just check the references for simplicity
        try {
            ClientSocketHandler anchorHandler=this.spt.getAnchorNodeHandler();
            if (receivedHandler.isEmpty() || receivedHandler.get() != anchorHandler) {
                ok = anchorHandler.sendMessage(msg);
            }
        }catch(SpanningTreeNoAnchorNodeException e){
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Node has no associated handler for this node, do not forward along him", e);
        }

        // Forward along children
        for(ClientSocketHandler h : this.spt.getChildren()){
            if (receivedHandler.isEmpty() || receivedHandler.get() != h) {
                ok = h.sendMessage(msg) || ok;
            }
        }
        // TODO: fix corner cases of the network
        return ok;
    }

    public void sendMessage(Serializable content, NodeName destinationNodeName){
        ApplicationMessage message = new ApplicationMessage(content, this.name, destinationNodeName);
        this.forwardMessage(message, destinationNodeName);
    }

    private void forwardMessage(Message message, NodeName destinationNodeName){
        try {
            ClientSocketHandler handler = this.routingTable.getNextHop(destinationNodeName);
            handler.sendMessage(message);
        } catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Node not present in routing table", e);

            // Do discovery
            boolean ok = this.sendDiscoveryMessage(destinationNodeName);

            if(ok){
                try {
                    ClientSocketHandler handler = this.routingTable.getNextHop(destinationNodeName);
                    handler.sendMessage(message);
                } catch (RoutingTableNodeNotPresentException ex) {
                    LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here, the node should be present in the rt", ex);
                }
            }
            else {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "Node not reachable, careful", e);
            }
        }
    }
    private void forwardMessage(CallbackContent content){
        ApplicationMessage appMessage = (ApplicationMessage) content.getCallBackMessage();
        ThreadPool.submit(()->{this.forwardMessage(appMessage, appMessage.getReceiver());});
    }

    /**
     * Method invoked when we need to discover if a node is present in the network
     * @param destinationNodeName name of the node to discover
     * @return true if everything went well
     */
    private boolean sendDiscoveryMessage(NodeName destinationNodeName){
        MessageDiscovery msgd=new MessageDiscovery(this.name, destinationNodeName);

        boolean ok = this.forwardMessageAlongSPT(msgd, Optional.empty());

        if(!ok) return false;

        // Do the same as a synchronized message, wait for the reply
        // TODO: a bit of duplicated code
        Object lock = new Object();
        this.ackHandler.insertAckId(msgd.getSequenceNumber(), lock);

        try {
            // Wait for a timeout, if ack has been received then all good, else something bad happened.
            synchronized (lock){
                lock.wait(Config.getInt("network.ackTimeout"));
            }

            this.ackHandler.removeAckId(msgd.getSequenceNumber());
        }
        catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
            return false;
        }
        catch (AckHandlerAlreadyRemovedException e) {
            // If a runtime exception is thrown it means that the ack has been removed, so it has been received.
            LoggerManager.getInstance().mutableInfo("Ack received, can resume operations...", Optional.of(this.getClass().getName()), Optional.of("sendDiscoveryMessage"));
            return true;
        }
        // TODO: handle error of ack
        // If no exception is thrown then it means that
        LoggerManager.instanceGetLogger().log(Level.WARNING, "Timeout reached waiting for ack.");

        return false;
    }

    /**
     * Method invoked when a client handler receives a message. This method is SYNCHRONIZED on the entire object
     * to ensure that all the operations in it are atomic on all the structures of the manager.
     * @param m message received
     */
    synchronized void receiveMessage(Message m, ClientSocketHandler handler){

        // First of all check if the message needs ack, if it does then send back a message.
        if(m.needsAck()){
            // TODO: need error checking here, and decide what we should do.
            //  This message will be sent asynchronously, so we could also send it in another thread.
            LoggerManager.getInstance().mutableInfo("sending ack require from: "+ m.getClass().getName()+ m.getSequenceNumber(), Optional.of(this.getClass().getName()), Optional.of("receiveMessage"));
            handler.sendMessage(new MessageAck(m.getSequenceNumber()));
        }

        // Switch the ID of the message and do what you need to do:
        // TODO: I have an idea to possibly be more efficient.
        //  Maybe not all messages need a full locking on the object so you can pass it in the internal bits

        switch(m.getInternalID()){
            case MESSAGE_JOIN -> {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "An already known node tried to join the network.");
            }
            case MESSAGE_EXIT -> {
                try {
                    this.receiveExit((ExitMsg) m, handler);
                } catch (IOException e) {
                    // TODO: decide
                    LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                }
            }
            case MESSAGE_EXITNOTIFY -> {
                ExitNotify exitNotify = (ExitNotify) m;
                receiveExitNotify(exitNotify, handler);
            }
            case MESSAGE_JOINFORWARD -> {
                try {
                    this.receiveJoinForward((JoinForwardMsg) m, handler);
                } catch (IOException e) {
                    // TODO: decide
                    LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                }
            }
            case MESSAGE_DIRECTCONNECTION -> {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "An already known node tried to directly connect with this node.");
            }
            case MESSAGE_ACK -> {
                LoggerManager.getInstance().mutableInfo("ack received [sequence code: " + m.getSequenceNumber() + "]", Optional.of(this.getClass().getName()), Optional.of("receiveMessage"));
                // If the message received is an ack then remove it from the ack handler
                try {
                    this.ackHandler.removeAckId(m.getSequenceNumber());
                } catch (AckHandlerAlreadyRemovedException e) {
                    LoggerManager.getInstance().mutableInfo("Ack already removed from the ack map", Optional.of(this.getClass().getName()), Optional.of("receiveMessage"));
                    // LoggerManager.instanceGetLogger().log(Level.SEVERE, "Ack already removed from the ack map", e);
                }
            }
            case MESSAGE_PINGPONG -> {
                PingPongMessage pingPongMessage = (PingPongMessage) m;
                if(pingPongMessage.isFistPing()) {
                    try {
                        this.spt.addChild(handler);
                    } catch (SpanningTreeChildAlreadyPresentException e) {
                        // todo: decide
                        LoggerManager.instanceGetLogger().log(Level.SEVERE, "Spanning tree exception", e);
                    }
                    handler.startPingPong(false); //the client who send U the ping as U as father
                }
            }
            case MESSAGE_APP -> {
                ApplicationMessage app = (ApplicationMessage)m;
                if(app.getReceiver().equals(this.name)) {
                    Event messageInputChannel = handler.getMessageInputChannel();
                    messageInputChannel.publish(m);
                }else{
                    toForwardEvent.publish(app);
                    //this.forwardMessage(m,app.getReceiver());
                }
            }
            case MESSAGE_DISCOVERY -> {
                MessageDiscovery msgd = (MessageDiscovery) m;

                // Save in routing table
                try {
                    this.routingTable.addPath(msgd.getOriginName(), handler);
                } catch (RoutingTableNodeAlreadyPresentException e) {
                    LoggerManager.getInstance().mutableInfo( "Node already present. Do nothing", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
                    // I guess just do not do anything
                }

                // I need to check if I am the correct destination
                if(Objects.equals(msgd.getDestinationName(),this.name)){

                    // Send ack back
                    MessageDiscoveryReply msgdr = new MessageDiscoveryReply(msgd.getSequenceNumber(), this.name, msgd.getOriginName());
                    handler.sendMessage(msgdr);

                    // return, nothing else to do
                    return;
                }


                // just forward the signal along the spt. If it is in the routing table good, else along spt
                try{
                    ClientSocketHandler nextHandler = this.routingTable.getNextHop(msgd.getDestinationName());
                    nextHandler.sendMessage(msgd);
                }
                catch(RoutingTableNodeNotPresentException e){
                    LoggerManager.getInstance().mutableInfo( "Node not present, forwarding", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));

                    // Forward to all the handlers in the spt except the one you received it from
                    this.forwardMessageAlongSPT(msgd, Optional.of(handler));
                }
            }
            case MESSAGE_DISCOVERYREPLY -> {
                MessageDiscoveryReply msgdr = (MessageDiscoveryReply) m;
                LoggerManager.getInstance().mutableInfo("received discovery reply about "+ msgdr.getOriginName().getIP() +":"+ msgdr.getOriginName().getPort() +" from " + handler.getRemoteNodeName().getPort() + ":"+ handler.getRemoteNodeName().getIP(), Optional.of(this.getClass().getName()), Optional.of("receiveMessage"));

                // Save information in routing table
                try {
                    this.routingTable.addPath(msgdr.getOriginName(),handler);
                } catch (RoutingTableNodeAlreadyPresentException e) {
                    LoggerManager.getInstance().mutableInfo( "Node already present, do nothing.", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
                }

                // If I am the destination of the reply then notify my thread
                if(Objects.equals(msgdr.getDestinationName(),this.name)){
                    try {
                        this.ackHandler.removeAckId(msgdr.getSequenceNumber());
                    } catch (AckHandlerAlreadyRemovedException e) {
                        LoggerManager.instanceGetLogger().log(Level.SEVERE, "Ack already removed from the ack map", e);
                    }
                    return;
                }

                // Else I need to forward it
                // If it is in routing table then send it directly
                // TODO: can be refactored and merged with the case above
                try{
                    ClientSocketHandler nextHandler = this.routingTable.getNextHop(msgdr.getDestinationName());
                    nextHandler.sendMessage(msgdr);
                }catch(RoutingTableNodeNotPresentException e){
                    System.err.println("[ConnectionManager] Node not present, forwarding: " + e.getMessage());

                    this.forwardMessageAlongSPT(msgdr, Optional.of(handler));
                }
            }
            case SNAPSHOT_RESET_REQUEST -> {
                RestoreSnapshotRequest restoreSnapshotRequest = (RestoreSnapshotRequest) m;
                ThreadPool.submit(()->{this.receiveSnapshotRestoreRequest(restoreSnapshotRequest, handler);});
            }
            case SNAPSHOT_RESET_RESPONSE ->{
                RestoreSnapshotResponse restoreSnapshotResponse = (RestoreSnapshotResponse) m;
                this.receiveSnapshotRestoreResponse(restoreSnapshotResponse, handler);
            }
            case SNAPSHOT_RESET_AGREEMENT -> {
                RestoreSnapshotRequestAgreementResult result = (RestoreSnapshotRequestAgreementResult) m;
                this.receiveAgreementResult(result, handler);
            }
            case SNAPSHOT_TOKEN -> {
                LoggerManager.getInstance().mutableInfo("snapshot token received", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
                TokenMessage tokenMessage = (TokenMessage) m;
                String tokenName = tokenMessage.getSnapshotId()+"-"+tokenMessage.getSnapshotCreatorName().getIP()+"-"+tokenMessage.getSnapshotCreatorName().getPort();

                if (snapshotManager.manageSnapshotToken(tokenName, handler.getRemoteNodeName())) {
                    this.forwardToken(tokenMessage);
                }
            }
            case MESSAGE_NOTIMPLEMENTED -> {
                // TODO: decide, should be the same as default
            }
            case null, default -> {
                // TODO: decide
            }
        }

    }

    // <editor-fold desc="Static Getter">
    public RoutingTable getRoutingTable(){
        return routingTable;
    }

    public SpanningTree getSpt(){
        return this.spt;
    }
    // </editor-fold>
}
