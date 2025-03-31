package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.*;
import polimi.ds.dsnapshot.Connection.Messages.Exit.ExitMsg;
import polimi.ds.dsnapshot.Connection.Messages.Exit.ExitNotify;
import polimi.ds.dsnapshot.Connection.Messages.Join.DirectConnectionMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinForwardMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinMsg;
import polimi.ds.dsnapshot.Exception.*;

import java.io.IOException;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Snapshot.SnapshotManager;
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
    private final AtomicReference<RoutingTable> routingTable = new AtomicReference<>(new RoutingTable());
    private final AtomicReference<SpanningTree> spt = new AtomicReference<>(new SpanningTree());
    private final SnapshotManager snapshotManager = new SnapshotManager(this);//todo: implement pars Token
    /**
     * Reference to the handler of the acks
     */
    private final AckHandler ackHandler;
    /**
     * Name of this connection manager.
     * For the moment we can assume it is immutable
     */
    private final NodeName name;

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

        LoggerManager.getInstance().mutableInfo("ConnectionManager created successfully...", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
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
                    this.unNamedHandlerList.add(unhandler);
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

        LoggerManager.getInstance().mutableInfo("Sending a message to "+ip+":"+port+"...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));

        NodeName destNode = new NodeName(ip, port);

        try {
            LoggerManager.getInstance().mutableInfo("Checking the routing table for the next hop...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            ClientSocketHandler handler = this.routingTable.get().getNextHop(destNode);

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
        LoggerManager.getInstance().mutableInfo("Preparing for receiving an ack...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
        int seqn = m.getSequenceNumber();
        // Insert in the handler the number and the thread to wait
        this.ackHandler.insertAckId(seqn, Thread.currentThread());

        LoggerManager.getInstance().mutableInfo("Sending the message ...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
        boolean b = handler.sendMessage(m);

        if(!b) {
            LoggerManager.getInstance().mutableInfo("Something went wrong while sending the message...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return false;
        }

        LoggerManager.getInstance().mutableInfo("Sent, now waiting for ack...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));

        try {
            // Wait for a timeout, if ack has been received then all good, else something bad happened.
            this.wait(Config.getInt("network.ackTimeout"));
            // Once I have finished I have two possibilities. Either the ack has been removed from the list or not
            // If it has been removed then an exception is thrown.
            this.ackHandler.removeAckId(seqn);
        }
        catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
            return false;
        }
        catch (RuntimeException e) {
            // If a runtime exception is thrown it means that the ack has been removed, so it has been received.
            LoggerManager.getInstance().mutableInfo("Ack received, can resume operations...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return true;
        }
        // TODO: handle error of ack
        // If no exception is thrown then it means that
        LoggerManager.instanceGetLogger().log(Level.WARNING, "Timeout reached waiting for ack.");
        throw new ConnectionException("[ConnectionManager] Timeout reached waiting for ack");
    }

    private void sendBroadcastMsg(Message msg){
        for(ClientSocketHandler h : this.spt.get().getChildren()) {
            h.sendMessage(msg);
        }
        ClientSocketHandler anchorNodeHandler = this.spt.get().getAnchorNodeHandler();
        if(anchorNodeHandler != null) anchorNodeHandler.sendMessage(msg);
    }

    /**
     * Processes a direct connection message received from another node
     * @param nodeName Name of the node to be added to the table
     * @param handler handler of the socket
     * @throws RoutingTableNodeAlreadyPresentException if the ip address is already in the routing table
     */
    private void addNewRoutingTableEntry(NodeName nodeName, ClientSocketHandler handler) throws RoutingTableNodeAlreadyPresentException {
        this.routingTable.get().addPath(nodeName,handler);
    }

    // <editor-fold desc="Join procedure">
    /**
     * Establishes a connection to an anchor node in the network by creating a socket connection.
     * Sends a `JoinMsg` message to the specified node in order to initiate the join process.
     * @param anchorName name of the anchor node to connect to
     * @throws IOException if an I/O error occurs during socket connection or communication
     */
    public synchronized void joinNetwork(NodeName anchorName) throws IOException {
        Socket socket = new Socket(anchorName.getIP(),anchorName.getPort());
        //create socket for the anchor node, add to direct connection list and save as anchor node
        ClientSocketHandler handler = new ClientSocketHandler(socket, anchorName,this);
        ThreadPool.submit(handler);
        //send join msg to anchor node & wait for ack
        JoinMsg msg = new JoinMsg(this.name);
        try {

            LoggerManager.getInstance().mutableInfo("Joining the network to anchor "+anchorName.getIP()+":"+anchorName.getPort(), Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));

            boolean ret = false;

            // TODO: refactor a bit with exceptions
            while(!ret){
                ret=this.sendMessageSynchronized(msg,handler);

                if(!ret) LoggerManager.getInstance().mutableInfo("Something went wrong. Maybe it was a lock problem, so retry...", Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));
            }

            LoggerManager.getInstance().mutableInfo("Ack from anchor received!", Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));

            // If everything went well continue...

            // Add it as a parent in the spt
            this.spt.get().setAnchorNodeHandler(handler);
            // Add it to the active list of handlers
            this.handlerList.add(handler);
            // Add it to the routing table
            this.routingTable.get().addPath(anchorName, handler);
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
            LoggerManager.getInstance().mutableInfo("Join request received from node "+joinMsg.getJoinerName().getIP()+":"+joinMsg.getJoinerName().getPort(), Optional.of(this.getClass().getName()), Optional.of("receiveNewJoinMessage"));
            // Create the new handler
            ClientSocketHandler handler = new ClientSocketHandler(unnamedHandler, joinMsg.getJoinerName(), this);
            // Add it in the current handler list
            this.handlerList.add(handler);
            // Remove the unnamed handler from the list. The garbage collector will do its job later
            this.unNamedHandlerList.remove(unnamedHandler);
            // Add a new routing table entry
            this.addNewRoutingTableEntry(handler.getRemoteNodeName(), handler);
            // Since it is a new direct connection I need to add it to the spt
            // this.spt.get().getChildren().add(handler);
            // Submit the new handler to the thread pool
            ThreadPool.submit(handler);

            // Since the join is a synchronous process we need to send back the ack
            MessageAck msgAck = new MessageAck(joinMsg.getSequenceNumber());
            boolean ret=false;
            // TODO: refactor a bit with exceptions
            while(!ret){
                ret=handler.sendMessage(msgAck);
                if(!ret) LoggerManager.getInstance().mutableInfo("Something went wrong. Maybe it was a lock problem, so retry...", Optional.of(this.getClass().getName()), Optional.of("joinNetwork"));
            }

            //forward join notify to active neighbours
            JoinForwardMsg m = new JoinForwardMsg(joinMsg.getJoinerName());

            LoggerManager.getInstance().mutableInfo("Forwarding info to the other nodes.", Optional.of(this.getClass().getName()), Optional.of("receiveNewJoinMessage"));

            for(ClientSocketHandler h : this.handlerList){
                if(h!=handler) h.sendMessage(m);
            }

        } catch (RoutingTableNodeAlreadyPresentException e) {
            //TODO manage: if I receive a join from a node already in the routing table (wtf)
            return;
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
            LoggerManager.getInstance().mutableInfo("Direct connection request received from node "+directConnectionMsg.getJoinerName().getIP()+":"+directConnectionMsg.getJoinerName().getPort(), Optional.of(this.getClass().getName()), Optional.of("receiveNewDirectConnectionMessage"));
            //create socket for the anchor node, add to direct connection list and save as anchor node
            ClientSocketHandler handler = new ClientSocketHandler(unnamedHandler, directConnectionMsg.getJoinerName(),this);
            ThreadPool.submit(handler);
            // Remove the unnamed handler from the list. The garbage collector will do its job later
            this.unNamedHandlerList.remove(unnamedHandler);
            // Add a new routing table entry
            this.addNewRoutingTableEntry(handler.getRemoteNodeName(), handler);
            // Since it is not a direct connection it does not need to be added to the spt
            // Submit the new handler to the thread pool
            ThreadPool.submit(handler);

            // Since the direct connection is not synchronous we do not need to send back an ack

            // And no forwarding

        } catch (RoutingTableNodeAlreadyPresentException e) {
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

        try {
            if(randomValue < this.directConnectionProbability){
                // Open a new socket
                Socket socket = new Socket(msg.getJoinerName().getIP(),msg.getJoinerName().getPort());
                // Create the new handler
                ClientSocketHandler joinerHandler = new ClientSocketHandler(socket, msg.getJoinerName(),this);
                ThreadPool.submit(joinerHandler);
                // Add it in the current handler list
                this.handlerList.add(joinerHandler);
                //send to joiner a message to create a direct connection
                joinerHandler.sendMessage(new DirectConnectionMsg(this.name));
                //add node in routing table
                this.addNewRoutingTableEntry(msg.getJoinerName(), joinerHandler);
                // Do not to spt as it is not a direct connection
            }else {
                //creating undirected path to the joiner node with the anchor node
                this.routingTable.get().addPath(msg.getJoinerName(),handler);
            }
        } catch (RoutingTableNodeAlreadyPresentException e) {
            // Not much we can do
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here, a node already in the routing table asked to connect", e);
        }
    }
    // </editor-fold>

    // <editor-fold desc="Exit procedure">
    // TODO: fix this too
    public synchronized void exitNetwork() throws IOException{
        //reassign all child to the current anchor node of the exiting node
        ClientSocketHandler handler = this.spt.get().getAnchorNodeHandler();

        //send exit message to all child

        ExitMsg m = new ExitMsg(handler.getRemoteNodeName());
        this.sendBroadcastMsg(m);

        //clear handler list
        this.unNamedHandlerList.clear();

        //clear routing table
        this.routingTable.get().clearTable();
    }

    private void receiveExit(ExitMsg msg, ClientSocketHandler handler) throws IOException {
        try {
            this.routingTable.get().removePath(handler.getRemoteNodeName());
            this.routingTable.get().removeAllIndirectPath(handler);
            handler.close();

            ClientSocketHandler anchorNodeHandler = this.spt.get().getAnchorNodeHandler();
            if(handler == anchorNodeHandler){
                //reassign anchor node
                // There has to be a better way of doing it
                this.spt.get().setAnchorNodeHandler(null);
                this.sendExitNotify(handler.getRemoteNodeName());
                this.newAnchorNode(msg);
            }else if(anchorNodeHandler != null){
                //forward exit notify to anchor node only
                this.sendExitNotify(handler.getRemoteNodeName());

                //TODO send to anchor node only isn't enough, discuss how to avoid message loops
            }
        // TODO: explicit exceptions ? Which is this one ?
        } catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "We should not be here, a node not present in the routing table send an exit", e);
            //TODO if ip not in routing table
        }
    }
    /**
     * Handles the assignment of a new anchor node when the current anchor node exits the network.
     * This method determines whether a path to the new anchor exists in the routing table
     * and establishes a direct connection if necessary.
     * @param msg     the {@link ExitMsg} containing the IP and port of the new anchor node.
     */
    private void newAnchorNode(ExitMsg msg) throws IOException {
        ClientSocketHandler newAnchorNextHop;
        try {
            // Attempt to fetch the next hop in the routing table for the new anchor node.
            newAnchorNextHop = this.routingTable.get().getNextHop(msg.getNewAnchorName());
        } catch (RoutingTableNodeNotPresentException e) {
            // No path to reach the new anchor node, establish a direct connection.
            this.joinNetwork(msg.getNewAnchorName());
            return;
        }

        // Check if there is already a direct connection with the new anchor node.
        if (newAnchorNextHop.getRemoteNodeName().equals(msg.getNewAnchorName())) {
            //TODO: there is already a direct cnt between this node and the anchor -> start ping pong
            //set new anchor node
            this.spt.get().setAnchorNodeHandler(newAnchorNextHop);
            return;
        }
        // No direct connection with the new anchor node; establish one.
        this.joinNetwork(msg.getNewAnchorName());
    }

    private void sendExitNotify(NodeName nodeName){
        ExitNotify exitNotify = new ExitNotify(nodeName);
        sendBroadcastMsg(exitNotify);
    }

    // </editor-fold>

    // <editor-fold desc="Snapshot procedure">
    private void forwardToken(TokenMessage tokenMessage, ClientSocketHandler inputHandler){
        for(ClientSocketHandler h : this.handlerList){
            if(!Objects.equals(h, inputHandler)){//todo: verify
                h.sendMessage(tokenMessage);
            }
        }
    }

    public void startNewSnapshot(){
        //snapshot preparation
        String CHARACTERS = Config.getString("snapshot.codeAdmissibleChars");
        SecureRandom RANDOM = new SecureRandom();
        String snapshotCode= RANDOM.ints(Config.getInt("snapshot.uniqueCodeSize"), 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt)
                .map(String::valueOf)
                .collect(Collectors.joining());

        TokenMessage tokenMessage = new TokenMessage(snapshotCode, name);

        //start snapshot locally
        snapshotManager.manageSnapshotToken(snapshotCode,name);

        //notify the rest of the network
        this.sendBroadcastMsg(tokenMessage);
    }
    // </editor-fold>

    /**
     * Method used for forwarding a message not contained in the routing table.
     * It sends the message along all paths of the spanning tree saved
     * @param msg message to forward
     * @param receivedHandler handler from which the message has been received
     * @return true if everything went well.
     */
    private boolean forwardMessageAlongSPT(Message msg, ClientSocketHandler receivedHandler){
        boolean ok = true;

        // I can just check the references for simplicity
        if(receivedHandler!=this.spt.get().getAnchorNodeHandler() && this.spt.get().getAnchorNodeHandler()!=null){
            ok = this.spt.get().getAnchorNodeHandler().sendMessage(msg);
        }

        for(ClientSocketHandler h : this.spt.get().getChildren()){
            if(receivedHandler!=h) {
                ok = h.sendMessage(msg) || ok;
            }
        }
        // TODO: fix corner cases of the network
        return ok;
    }

    public void sendMessage(Message message, NodeName destinationNodeName){

        boolean ok = true;

        do {
            try {
                ClientSocketHandler handler = this.routingTable.get().getNextHop(destinationNodeName);
                handler.sendMessage(message);
            } catch (RoutingTableNodeNotPresentException e) {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "Node not present in routing table", e);
                // If everyhting went well then we can send again the message
                ok = this.sendDiscoveryMessage(destinationNodeName);
            }
        }while(!ok);
    }

    /**
     * Useful method to send messages along the spt
     * @param msg message to be sent
     * @return true if everything went well
     */
    // TODO: create and throw some exceptions here
    private boolean sendAlongSPT(Message msg){
        // TODO: what if anchor node is null? Need to notify the application
        boolean ok = this.spt.get().getAnchorNodeHandler().sendMessage(msg);

        // TODO: case in which no children
        for(ClientSocketHandler h : this.spt.get().getChildren()){
            ok = h.sendMessage(msg) || ok;
            LoggerManager.getInstance().mutableInfo( "Sending message to all children", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
        }

        return ok;
    }

    /**
     * Method invoked when we need to discover if a node is present in the network
     * @param destinationNodeName name of the node to discover
     * @return true if everything went well
     */
    private synchronized boolean sendDiscoveryMessage(NodeName destinationNodeName){
        MessageDiscovery msgd=new MessageDiscovery(this.name, destinationNodeName);

        boolean ok = this.sendAlongSPT(msgd);

        if(!ok) return false;

        // Do the same as a synchronized message, wait for the reply
        // TODO: a bit of duplicated code
        this.ackHandler.insertAckId(msgd.getSequenceNumber(), Thread.currentThread());

        try {
            // Wait for a timeout, if ack has been received then all good, else something bad happened.
            // TODO: wrap in constant
            this.wait(5000);
        } catch (InterruptedException e) {
            // Here some other thread will have removed the sequence number from the set so it means that the ack
            // Has been received correctly, and it is safe to return
            // Still a bit ugly that you capture an exception and resume correctly...
            LoggerManager.getInstance().mutableInfo("Ack received, operations can be resumed", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
            return true;
        }

        // If the method is not interrupted it means that the ack has not been received
        // TODO: handle error of ack
        LoggerManager.instanceGetLogger().log(Level.SEVERE, "Ack not received. Maybe it was lost ?");

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
                //TODO: I think here update the routing table
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
                this.ackHandler.removeAckId(m.getSequenceNumber());
            }
            case MESSAGE_PINGPONG -> {
                PingPongMessage pingPongMessage = (PingPongMessage) m;
                if(pingPongMessage.isFistPing()) {
                    try {
                        this.spt.get().addChild(handler);
                    } catch (SpanningTreeException e) {
                        // todo: decide
                        LoggerManager.instanceGetLogger().log(Level.SEVERE, "Spanning tree exception", e);
                    }
                    handler.startPingPong(false); //the client who send U the ping as U as father
                }
            }
            case MESSAGE_APP -> {
                //todo if message require to be forward
                Event messageInputChannel = handler.getMessageInputChannel();
                messageInputChannel.publish(m);
            }
            case MESSAGE_DISCOVERY -> {
                MessageDiscovery msgd = (MessageDiscovery) m;

                // Save in routing table
                try {
                    this.routingTable.get().addPath(msgd.getOriginName(), handler);
                } catch (RoutingTableNodeAlreadyPresentException e) {
                    LoggerManager.getInstance().mutableInfo( "Node already present. Do nothing", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
                    // I guess just do not do anything
                }

                // I need to check if I am the correct destination
                if(msgd.getDestinationName()==this.name){

                    // Send ack back
                    MessageDiscoveryReply msgdr = new MessageDiscoveryReply(msgd.getSequenceNumber(), this.name, msgd.getDestinationName());
                    handler.sendMessage(msgdr);

                }
                // Else just forward the signal
                else{
                    ClientSocketHandler nextHandler=null;
                    try{
                        nextHandler = this.routingTable.get().getNextHop(msgd.getDestinationName());
                        nextHandler.sendMessage(msgd);
                    }
                    catch(RoutingTableNodeNotPresentException e){
                        LoggerManager.getInstance().mutableInfo( "Node not present, forwarding", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));

                        // Forward to all the handlers in the spt except the one you received it from
                        this.forwardMessageAlongSPT(msgd, handler);
                    }
                }
            }
            case MESSAGE_DISCOVERYREPLY -> {
                MessageDiscoveryReply msgdr = (MessageDiscoveryReply) m;

                // Save information in routing table
                try {
                    this.routingTable.get().addPath(msgdr.getOriginName(),handler);
                } catch (RoutingTableNodeAlreadyPresentException e) {
                    LoggerManager.getInstance().mutableInfo( "Node already present, do nothing.", Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
                }

                // If I am the destination of the reply then notify my thread
                if(msgdr.getDestinationName()==this.name){
                    this.ackHandler.removeAckId(msgdr.getSequenceNumber());
                }
                // Else I need to forward it
                else{
                    // If it is in routing table then send it directly
                    // TODO: can be refactored and merged with the case above
                    ClientSocketHandler nextHandler=null;
                    try{
                        nextHandler = this.routingTable.get().getNextHop(msgdr.getDestinationName());
                        nextHandler.sendMessage(msgdr);
                    }catch(RoutingTableNodeNotPresentException e){
                        System.err.println("[ConnectionManager] Node not present, forwarding: " + e.getMessage());

                        this.forwardMessageAlongSPT(msgdr, handler);
                    }
                }
            }
            case SNAPSHOT_TOKEN -> {
                TokenMessage tokenMessage = (TokenMessage) m;
                String tokenName = tokenMessage.getSnapshotId()+"_"+tokenMessage.getSnapshotCreatorName().getIP()+"_"+tokenMessage.getSnapshotCreatorName().getPort();

                if (snapshotManager.manageSnapshotToken(tokenName, handler.getRemoteNodeName())) {
                    this.forwardToken(tokenMessage, handler);
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
    synchronized public RoutingTable getRoutingTable(){
        return routingTable.get();
    }

    synchronized public SpanningTree getSpt(){
        return spt.get();
    }
    // </editor-fold>
}
