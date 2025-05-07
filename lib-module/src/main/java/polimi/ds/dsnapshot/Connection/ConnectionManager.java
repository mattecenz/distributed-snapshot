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
import java.net.*;
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
import polimi.ds.dsnapshot.Exception.ExportedException.*;
import polimi.ds.dsnapshot.Exception.RoutingTable.RoutingTableNodeAlreadyPresentException;
import polimi.ds.dsnapshot.Exception.RoutingTable.RoutingTableNodeNotPresentException;
import polimi.ds.dsnapshot.Exception.SPT.SpanningTreeChildAlreadyPresentException;
import polimi.ds.dsnapshot.Exception.SPT.SpanningTreeNoAnchorNodeException;
import polimi.ds.dsnapshot.Exception.Snapshot.Snapshot2PCException;
import polimi.ds.dsnapshot.Exception.Snapshot.SnapshotPendingRequestManagerException;
import polimi.ds.dsnapshot.Exception.Snapshot.SnapshotRestoreException;
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
     * List of active connections which do not have yet a name.
     */
    private final List<UnNamedSocketHandler> unNamedHandlerList;
    /**
     * List of all the active connections in the network.
     */
    private final List<ClientSocketHandler> handlerList;
    /**
     * Routing table of the application.
     */
    private final RoutingTable routingTable = new RoutingTable();
    /**
     * Spanning tree of the application.
     */
    private final SpanningTree spt = new SpanningTree();
    /**
     * Manager of the snapshot called whenever a creation/restore procedure is invoked.
     */
    private final SnapshotManager snapshotManager = new SnapshotManager(this);
    /**
     * Manager of the pending requests waiting for the responses of the 2PC phase of the snapshot.
     */
    private SnapshotPendingRequestManager snapshotPendingRequestManager = null;
    /**
     * Reference to the handler of the acks.
     */
    private final AckHandler ackHandler;
    /**
     * Name of this connection manager.
     */
    private final NodeName name;
    /**
     * Event used when needing to forward an application message towards another peer.
     */
    private Event toForwardEvent;
    /**
     * Socket created for receiving inbound messages.
     * It is not final as it might fail to connect to the port.
     */
    private ServerSocket serverSocket;
    /**
     * Reference to the manager which handles the blocking mode of the application.
     */
    //private boolean panicMode=false;
    private PanicManager panicManager = new PanicManager();
    /**
     * Name of the parent who crashed. Useful for reconnection procedures.
     */
    private Optional<NodeName> nameOfCrashedParent = Optional.empty();
    /**
     * Name of the children who crashed. Useful for reconnection procedures.
     */
    private final List<NodeName> nameOfCrashedChildren = new ArrayList<>();
    /**
     * Probability of establishing a direct connection when a new node enters the network.
     */
    double directConnectionProbability = Config.getDouble("network.directConnectionProbability");

    /**
     * Constructor of the connection manager.
     * @param ip IP of the client.
     * @param port Port of the client where the socket is opened.
     * @throws DSException DSPortAlreadyInUseException, if the port is already used by someone else.
     */
    public ConnectionManager(String ip, int port) throws DSException{
        this.unNamedHandlerList = new ArrayList<>();
        this.handlerList = new ArrayList<>();
        this.ackHandler = new AckHandler();

        // TODO: for the moment this is useless, maybe it will be useful later.
//        // Default value if something goes wrong
//        String thisIP = "127.0.0.1";
//        try{
//            InetAddress localHost = InetAddress.getLocalHost();
//            // Get the IP address as a string
//            thisIP = localHost.getHostAddress();
//        }
//        catch(UnknownHostException e){
//            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Host not connected to network, cannot do anything:", e);
//        }
        this.name = new NodeName(ip, port);

        LoggerManager.getInstance().mutableInfo("ConnectionManager created successfully. My name is: " + ip + ":" + port, Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));

        // Create the socket if possible.
        try{
            this.serverSocket =new ServerSocket(port);
            LoggerManager.getInstance().mutableInfo("Created listening socket on port " + this.name.getPort() + " ...", Optional.of(this.getClass().getName()), Optional.of("start"));
        }
        catch(BindException e){
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "The port you want to use is already occupied! ", e);
            throw new DSPortAlreadyInUseException();
        }
        catch(IOException e){
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
            // TODO: what to do ?
        }
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

        LoggerManager.getInstance().mutableInfo("ConnectionManager created successfully. My name is: "+this.name.getIP()+":"+this.name.getPort() , Optional.of(this.getClass().getName()), Optional.of("ConnectionManager"));
    }

    /**
     * Getter of the name of the node.
     * @return The name of the node.
     */
    public NodeName getName() {
        return name;
    }

    /**
     * Method called when the connection manager is ready to start normal operations.
     * It will launch the thread which waits for inbound messages.
     */
    public void start(){
        LoggerManager.getInstance().mutableInfo("Preparing the thread...", Optional.of(this.getClass().getName()), Optional.of("start"));

        // This start has to launch another thread.

        Thread t = new Thread(()->{
            try{
                while(true){
                    LoggerManager.getInstance().mutableInfo("Waiting for connection...", Optional.of(this.getClass().getName()), Optional.of("start"));
                    Socket socket = this.serverSocket.accept();
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
            }
            catch(IOException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                // TODO: what to do ?
            }
            // Here the serverSocket is closed
            LoggerManager.getInstance().mutableInfo("Shutting down...", Optional.of(this.getClass().getName()), Optional.of("start"));
        });

        LoggerManager.getInstance().mutableInfo("Launching the thread...", Optional.of(this.getClass().getName()), Optional.of("start"));

        t.start();
    }

    /**
     * Method which sends a message and waits for an ack to be received.
     * Can be exposed to the application.
     * @param m Message to be sent.
     * @param ip IP of the node to send the message to.
     * @param port Port of the node to send the message to.
     * @return True if everything went well.
     */
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
        } catch (SocketClosedException e) {
            LoggerManager.getInstance().mutableInfo("The socket has been closed. It is not possible to send messages anymore.", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return false;
        } catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "RoutingTableNodeNotPresentException", e);
            return false;
        } catch (AckTimeoutExpiredException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "ConnectionException", e);
            //todo: ack not received
            return false;
        }
    }

    /**
     * Method which sends a message and waits for an ack to be received.
     * @param m Message to be sent.
     * @param handler Socket handler where the message is sent.
     * @return True if everything went well.
     * @throws AckTimeoutExpiredException If the ack did not arrive in time.
     * @throws SocketClosedException If the socket has already been closed.
     */
    protected boolean sendMessageSynchronized(Message m, ClientSocketHandler handler) throws AckTimeoutExpiredException, SocketClosedException {

        LoggerManager.getInstance().mutableInfo("Sending a message: "+ m.getClass().getName() +" to "+handler.getRemoteNodeName().getIP()+":"+handler.getRemoteNodeName().getPort()+"...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
        LoggerManager.getInstance().mutableInfo("Preparing for receiving an ack...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
        int seqn = m.getSequenceNumber();
        // Insert in the handler the number and the thread to wait
        Object lock=new Object();
        this.ackHandler.insertAckId(seqn, lock);

        try{
            LoggerManager.getInstance().mutableInfo("Sending the message ...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            handler.sendMessage(m);
            LoggerManager.getInstance().mutableInfo("Sent, now waiting for ack...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            // Wait for a timeout, if ack has been received then all good, else something bad happened.
            synchronized (lock) {
                lock.wait(Config.getInt("network.ackTimeout"));
            }
            // Once I have finished I have two possibilities. Either the ack has been removed from the list or not
            // If it has been removed then an exception is thrown.
            this.ackHandler.removeAckId(seqn);
        } catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
            return false;
        }
        catch (AckHandlerAlreadyRemovedException e) {
            // If a runtime exception is thrown it means that the ack has been removed, so it has been received.
            LoggerManager.getInstance().mutableInfo("Ack received, can resume operations...", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return true;
        }

        // TODO: handle error of ack
        LoggerManager.instanceGetLogger().log(Level.WARNING, "Timeout reached waiting for ack.");
        throw new AckTimeoutExpiredException();
    }

    /**
     * Add a new entry to the current routing table.
     * @param nodeName Name of the node to be added to the table.
     * @param handler Handler of the socket.
     * @throws RoutingTableNodeAlreadyPresentException If the node name is already in the routing table.
     */
    private void addNewRoutingTableEntry(NodeName nodeName, ClientSocketHandler handler) throws RoutingTableNodeAlreadyPresentException {
        this.routingTable.addPath(nodeName,handler);
    }

    // <editor-fold desc="Join procedure">
    /**
     * Establishes a connection to an anchor node in the network by creating a socket connection.
     * Sends a `JoinMsg` message to the specified node in order to initiate the join process.
     * @param anchorName Name of the anchor node to connect to.
     * @throws DSException If something unexpected has happened (either DSMessageToMyselfException or DSNodeUnreachableException or DSConnectionUnavailableException).
     */
    public void joinNetwork(NodeName anchorName) throws DSException {
        if(anchorName.equals(this.name)) throw new DSMessageToMyselfException();

        JoinMsg msg = new JoinMsg(this.name);
        joinNetwork(anchorName,msg);
    }

    /**
     * Private method for joining the network used only for internal purposes.
     * @param anchorName Name of the anchor node to connect to.
     * @param joinMsg Join message to be sent.
     * @throws DSException If something unexpected has happened (either DSNodeUnreachableException or DSConnectionUnavailableException).
     */
    private void joinNetwork(NodeName anchorName, JoinMsg joinMsg) throws DSException {
        try {

            Socket socket = new Socket(anchorName.getIP(),anchorName.getPort());
            //create socket for the anchor node, add to direct connection list and save as anchor node
            ClientSocketHandler handler = new ClientSocketHandler(socket, anchorName, this, true);
            ThreadPool.submit(handler);
            //send join msg to anchor node & wait for ack

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
        }catch(ConnectException e){
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "The node you are trying to connect to is unreachable!", e);
            throw new DSNodeUnreachableException();
        } catch(IOException e){
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            // Since the handler will crash just put application in panic mode
            this.initiatePanicMode();
            throw new DSConnectionUnavailableException();
        }
        catch (AckTimeoutExpiredException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Error waiting for ack:", e);
            throw new DSConnectionUnavailableException();
        } catch (RoutingTableNodeAlreadyPresentException e) {
            // Should be impossible to reach this exception
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Node already present in routing table: ", e);
        }
    }

    /**
     * Method used when the anchor node has crashed.
     * It tries to reconnect to it automatically and re-initiate a join procedure.
     * @throws DSException If the node is unavailable.
     */
    public void reconnectToAnchor() throws DSException  {
        if(this.nameOfCrashedParent.isEmpty()) {
            LoggerManager.getInstance().mutableInfo("The parent has not crashed, do nothing.", Optional.of(this.getClass().getName()), Optional.of("reconnectToAnchor"));
            throw new DSParentNotCrashedException();
        }

        // // This method works also when in panic mode. So try to reconnect if possible
        this.joinNetwork(this.nameOfCrashedParent.get());

        // Once you have successfully reconnected you need to send a message to him telling him that somebody crashed
        // TODO: decide a bit if its ok to leave it commented
//        try {
//            this.spt.getAnchorNodeHandler().sendMessage(new MessageNodeCrashed());
//        } catch (SocketClosedException | SpanningTreeNoAnchorNodeException e) {
//            // Nothing to do
//        }
    }

    /**
     * Handles a join request received from another node in the network.
     * In case of a crashed network it will only accept joins if the node was previously connected.
     * It is a synchronized method.
     * @param joinMsg Join message received.
     * @param unnamedHandler The unnamed socket handler managing the client communication for the incoming connection.
     */
    synchronized void receiveNewJoinMessage(JoinMsg joinMsg, UnNamedSocketHandler unnamedHandler) {
        if(this.panicManager.isLocked()) {

            if (this.nameOfCrashedChildren.contains(joinMsg.getJoinerName())){
                LoggerManager.getInstance().mutableInfo("A saved children wants to reconnect, I'll allow it this time.", Optional.of(this.getClass().getName()), Optional.of("receiveNewJoinMessage"));
                this.nameOfCrashedChildren.remove(joinMsg.getJoinerName());
            }
            else {
                LoggerManager.getInstance().mutableInfo("Panic mode activated, cannot accept new connections anymore...", Optional.of(this.getClass().getName()), Optional.of("receiveNewJoinMessage"));
                // Dereference the connection
                this.unNamedHandlerList.remove(unnamedHandler);
                return;
            }
        }

        try {
            ClientSocketHandler handler = this.receiveAdoptionOrJoinRequest(joinMsg, unnamedHandler);
            this.sendJoinForwardMsg(joinMsg,handler);

            // This is not necessary but it is useful to force the child in panic mode.
            // TODO: decide
//            if(this.panicMode) {
//                handler.sendMessage(new MessageNodeCrashed());
//            }
        } catch (RoutingTableNodeAlreadyPresentException e) {//TODO: decide
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Node already exists in routing table: ", e);
        }
    }

    /**
     * Method which handles the new connection of a node (either a join or a direct connection).
     * It sends the ack back to the original sender, and notifies all the other nodes in the routing table
     * that a new join has occurred.
     * It is a synchronized method.
     * @param joinMsg Join message received.
     * @param unnamedHandler Unnamed handler from where the connection has been activated.
     * @return The new socket handler.
     * @throws RoutingTableNodeAlreadyPresentException If the node is already in the routing table.
     */
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

        try {
            handler.sendMessage(msgAck);
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            this.initiateCrashProcedure(handler);
        }

        return handler;
    }

    /**
     * Method to forward the join message towards the other nodes in the routing table.
     * @param joinMsg Join message received.
     * @param handler New handler where the connection has been created. The method avoids sending the message to him.
     */
    private void sendJoinForwardMsg(JoinMsg joinMsg, ClientSocketHandler handler) {
        //forward join notify to active neighbours
        JoinForwardMsg m = new JoinForwardMsg(joinMsg.getJoinerName());

        LoggerManager.getInstance().mutableInfo("Forwarding info to the other nodes.", Optional.of(this.getClass().getName()), Optional.of("receiveNewJoinMessage"));

        for (ClientSocketHandler h : this.handlerList) {
            if (h != handler) {
                try {
                    h.sendMessage(m);
                } catch (SocketClosedException e) {
                    LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
                    this.initiateCrashProcedure(handler);
                }
            }
        }
    }

    /**
     * Handles a direct connection request received from another node in the network.
     * It is a synchronized method.
     * @param directConnectionMsg Message received.
     * @param unnamedHandler The unnamed socket handler managing the client communication for the incoming connection.
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
            synchronized (this.handlerList) {
                this.handlerList.add(handler);
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
     * With a certain probability it will activate a new connection, else just saves the path in the routing table.
     * @param msg Join forward message received.
     * @param handler The socket handler where the message has been received.
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
                ClientSocketHandler joinerHandler = new ClientSocketHandler(socket, msg.getJoinerName(),this, true);
                ThreadPool.submit(joinerHandler);
                // Add it in the current handler list
                synchronized (this.handlerList) {
                    this.handlerList.add(joinerHandler);
                }
                //send to joiner a message to create a direct connection
                joinerHandler.sendMessage(new DirectConnectionMsg(this.name));
                //add node in routing table
                this.addNewRoutingTableEntry(msg.getJoinerName(), joinerHandler);
                // Do not to spt as it is not a direct connection
            }else {
                //creating undirected path to the joiner node with the anchor node
                this.routingTable.addPath(msg.getJoinerName(),handler);
            }
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            this.initiateCrashProcedure(handler);
        } catch (RoutingTableNodeAlreadyPresentException e) {
            // Not much we can do
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here, a node already in the routing table asked to connect", e);
        }
    }
    // </editor-fold>

    /**
     * Method called by the application when the client wants to gracefully leave the network.
     * It will notify all the other nodes about the decision and terminate all active connections.
     * If the panic mode is activated it is not possible to leave the network.
     * @throws DSException If some error occurred.
     */
    // <editor-fold desc="Exit procedure">
    public synchronized void exitNetwork() throws DSException {
        if(this.panicManager.isLocked()) {
            LoggerManager.getInstance().mutableInfo("Panic mode activated, cannot manually leave the network anymore...", Optional.of(this.getClass().getName()), Optional.of("exitNetwork"));
            throw new DSNetworkCrashedException();
        }

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
        try {
            this.forwardMessageAlongSPT(m, Optional.empty());
            LoggerManager.getInstance().mutableInfo("send exit msg on spt", Optional.of(this.getClass().getName()), Optional.of("exitNetwork"));
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            // since the node is exiting the network just enter panic mode and do nothing else
            this.initiatePanicMode();
        }

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

    /**
     * Method called when a new exit message it is received.
     * It will clear all the connections which passed towards that node.
     * @param msg Exit message received.
     * @param handler Socket handler from where the exit was received.
     * @throws IOException If something goes wrong while closing the connections.
     */
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
     * Method which initiates the crash procedure whenever an anomaly is detected (socket forcefully closed or ping failed).
     * It will initiate the panic mode and leave open only the connections which exchange the ping pong information.
     * It is a synchronized procedure.
     * @param handler Handler crashed.
     */
    synchronized void initiateCrashProcedure(ClientSocketHandler handler) {
        // When a crash happens the safest thing to do is to block everything from executing.
        // Which means block the application from sending messages.
        // The easiest way to do it is by setting a shared variable to true which indicates that the application
        // has gone in a "panic" state. (The better way to do it would be to use a state pattern)

        // If the handler was the father, then save his name as this might be useful for reconnections
        // For direct connections do not save them.
        try {
            if(this.spt.getAnchorNodeHandler().equals(handler)) {
                this.nameOfCrashedParent=Optional.of(handler.getRemoteNodeName());
                this.spt.removeAnchorNodeHandler();
            }
        } catch (SpanningTreeNoAnchorNodeException e) {
            // Extreme case of the node without a father
            this.nameOfCrashedParent=Optional.empty();
        }

        if(this.spt.getChildren().contains(handler)){
            this.nameOfCrashedChildren.add(handler.getRemoteNodeName());
        }

        // Remove the handler from everywhere

        handler.stopPingPong();

        try {
            this.routingTable.removePath(handler.getRemoteNodeName());
        } catch (RoutingTableNodeNotPresentException e) {
            // Do nothing
        }

        this.routingTable.removeAllIndirectPath(handler);
        this.handlerList.remove(handler);
        handler.close();

        List<ClientSocketHandler> children = this.spt.getChildren();
        if(children.contains(handler)){
            this.spt.removeChild(handler);
        }

        this.initiatePanicMode();

    }

    /**
     * Method to only initiate a panic mode if some thread notices an anomaly when trying to use a socket handler.
     * It is a synchronized procedure.
     */
    synchronized void initiatePanicMode(){
        if(this.panicManager.isLocked()) return;

        this.panicManager.setPanicMode(true);
        // This socket will be closed now.
        // If multiple sockets crash at the same time only one will activate the panic mode.

        // When the mode is activated then send a message in broadcast to force the stop of operations

        MessageNodeCrashed m = new MessageNodeCrashed();

        try {
            ClientSocketHandler anchorHandler = this.spt.getAnchorNodeHandler();
            anchorHandler.sendMessage(m);
        } catch (SpanningTreeNoAnchorNodeException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Node has no associated handler for this node, do not forward along him", e);
        } catch (SocketClosedException e) {
            // Do nothing
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket of the anchor has been already closed", e);
        }

        // Forward along children
        for (ClientSocketHandler h : this.spt.getChildren()) {
            try{
                h.sendMessage(m);
            } catch (SocketClosedException e) {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket of a child has been already closed", e);
                // Do nothing, continue to forward
            }
        }

    }

    /**
     * Handles the assignment of a new anchor node when the current anchor node exits the network.
     * This method determines whether a path to the new anchor exists in the routing table
     * and establishes a direct connection if not present.
     * @param msg The Exit message containing the name of the new anchor node.
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

    /**
     * Method which creates a new direct connection towards the new anchor node.
     * It is the same procedure as the join.
     * @param nodeName Name of the new anchor node.
     */
    private void newAnchorNodeEstablishDirectConnection(NodeName nodeName)  {
        AdoptionRequestMsg msg = new AdoptionRequestMsg(this.name);
        ThreadPool.submit(()->{
            try {
                this.joinNetwork(nodeName,msg);
            }  catch (DSConnectionUnavailableException e) {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            } catch (DSNodeUnreachableException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE,"Node unreachable when establishing connection with new Anchor", e);
            } catch (DSException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE,"Generic DS exception ca+tured: ", e);
            }
        });
    }

    /**
     * Method which notifies the spanning tree of the graceful exit of a node.
     * @param nodeName Name of the node who left the network.
     * @param handler Optional handler (used for deciding if the connection has to be send towards that handler or not).
     */
    private void sendExitNotify(NodeName nodeName, Optional<ClientSocketHandler> handler){
        LoggerManager.getInstance().mutableInfo("send exit notify", Optional.of(this.getClass().getName()), Optional.of("sendExitNotify"));
        ExitNotify exitNotify = new ExitNotify(nodeName);
        try {
            this.forwardMessageAlongSPT(exitNotify, handler);
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            this.initiatePanicMode();
        }
    }

    /**
     * Method invoked when an exit notify is received.
     * It will remove all the paths from the routing table towards that node.
     * @param exitNotify Exit notify message containing the name of the node who left.
     * @param handler Handler from where the connection has been received.
     */
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

    /**
     * Method used when a snapshot token is received.
     * It forwards it towards all the outgoing channels.
     * @param tokenMessage Token received.
     */
    // <editor-fold desc="Snapshot procedure">
    private void forwardToken(TokenMessage tokenMessage){
        try {
            for (ClientSocketHandler h : this.handlerList) {
                LoggerManager.getInstance().mutableInfo("forwarding token to: "+ h.getRemoteNodeName().getIP() + ":" + h.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("forwardToken"));
                h.sendMessage(tokenMessage);
            }
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here. The network is assumed stable during the snapshot!", e);
            this.initiatePanicMode();
        }
    }

    /**
     * Method used by the application to retrieve all the available snapshots in which this node is involved.
     * @return A string containing a pretty description of all the snapshots.
     */
    public String getAvailableSnapshots(){ // Ideally synchronized would be better but its fine for now
        return this.snapshotManager.getAllSnapshotsOfNode(this.name);
    }

    /**
     * Method called when the application wants to start a new snapshot.
     * It generates the name of the snapshot, saves the state and forwards all the tokens.
     * It is an atomic operation.
     */
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
        this.forwardToken(tokenMessage);
    }
    // </editor-fold>

    /**
     * Method called when the application wants to restore a snapshot.
     * It initiates the 2PC procedure, locks the application and waits for the response.
     * If the 2PC is successful it starts the reconstruction procedure.
     * @param snapshotIdentifier Unique identifier of the snapshot to be restored.
     * @throws DSSnapshotRestoreLocalException If the local node had some problem.
     * @throws DSSnapshotRestoreRemoteException If the remote node had some problem.
     */
    // <editor-fold desc="restore Snapshot procedure">
    public void startSnapshotRestoreProcedure(SnapshotIdentifier snapshotIdentifier) throws DSSnapshotRestoreLocalException, DSSnapshotRestoreRemoteException {
        //lock
        this.panicManager.setSnapshotLock(true);

        //2pc
        LoggerManager.getInstance().mutableInfo("starting snapshot restore procedure (2PC)", Optional.of(this.getClass().getName()), Optional.of("startSnapshotRestoreProcedure"));
        RestoreSnapshotRequest restoreSnapshotRequest = new RestoreSnapshotRequest(snapshotIdentifier);
        try{
            this.snapshotManager.reEnteringNodeValidateSnapshotRequest(snapshotIdentifier);

            synchronized (this){
                LoggerManager.getInstance().mutableInfo("set snapshotPendingRequestManager", Optional.of(this.getClass().getName()), Optional.of("startSnapshotRestoreProcedure"));
                this.snapshotPendingRequestManager =  new SnapshotPendingRequestManager(Optional.empty(), snapshotIdentifier);
                this.fillPendingRequests(Optional.empty());
                this.forwardMessageAlongSPT(restoreSnapshotRequest, Optional.empty());
            }


            //wait for response
            Object lock = this.snapshotPendingRequestManager.getSnapshotLock();

                synchronized(lock){
                    lock.wait(Config.getInt("snapshot.snapshotRestore2PCTimeout"));

                    if(!this.snapshotPendingRequestManager.isEmpty(snapshotIdentifier)){
                        LoggerManager.instanceGetLogger().log(Level.WARNING,"snapshot procedure fail due to timeout expiration");
                        throw new DSSnapshotRestoreRemoteException();
                    }


                }
                synchronized (this) {
                    LoggerManager.getInstance().mutableInfo("reset snapshotPendingRequestManager", Optional.of(this.getClass().getName()), Optional.of("startSnapshotRestoreProcedure"));
                    this.snapshotPendingRequestManager = null;
                }
        } catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
            this.panicManager.setSnapshotLock(false);
            throw new DSSnapshotRestoreLocalException();
            //TODO: decide
        } catch (SnapshotPendingRequestManagerException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"received inconsistent snapshot response",e);
            this.panicManager.setSnapshotLock(false);
            throw new DSSnapshotRestoreLocalException();
            //TODO: decide
        } catch (Snapshot2PCException e){
            LoggerManager.instanceGetLogger().log(Level.SEVERE,"the 2PC failed: ",e);
            this.panicManager.setSnapshotLock(false);
            throw new DSSnapshotRestoreLocalException();
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE,"Socket closed during snapshot restore! ",e);
            this.panicManager.setSnapshotLock(false);
        }
    }

    /**
     * Method which constructs the pending requests.
     * It will create an handler for each node in the spt and wait for a response from it.
     * @param sender Optional name of the sender node.
     */
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

    /**
     * Method called when a request to restore a snapshot is received.
     * It checks internally if the snapshot can be restored, will propagate the request to all the other nodes in the spt
     * and wait for their response.
     * Finally, it sends back to the sender his final response.
     * @param restoreSnapshotRequest Message received.
     * @param sender Socket handler who sent the request.
     */
    private void receiveSnapshotRestoreRequest(RestoreSnapshotRequest restoreSnapshotRequest, ClientSocketHandler sender){
        //lock
        this.panicManager.setSnapshotLock(true);

        //2pc
        LoggerManager.getInstance().mutableInfo("the node has received a restore request", Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreRequest"));
        try {
            this.snapshotManager.validateSnapshotRequest(restoreSnapshotRequest);

            if(this.spt.isNodeLeaf()) {
                sender.sendMessage(new RestoreSnapshotResponse(restoreSnapshotRequest, true));
                return;
            }

            SnapshotIdentifier snapshotIdentifier = restoreSnapshotRequest.getSnapshotIdentifier();
            synchronized (this){
                LoggerManager.getInstance().mutableInfo("set snapshotPendingRequestManager", Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreRequest"));
                this.snapshotPendingRequestManager =  new SnapshotPendingRequestManager(Optional.ofNullable(sender), snapshotIdentifier);
                this.fillPendingRequests(Optional.ofNullable(sender.getRemoteNodeName()));

                this.forwardMessageAlongSPT(restoreSnapshotRequest, Optional.ofNullable(sender));
            }

            //wait for response
            Object lock = snapshotPendingRequestManager.getSnapshotLock();

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
        } catch (Snapshot2PCException e){
            // A bit of a nested exception
            try {
                sender.sendMessage(new RestoreSnapshotResponse(restoreSnapshotRequest, false));
            } catch (SocketClosedException ex) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE,"Socket closed during snapshot restore! ",e);
            }
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE,"Socket closed during snapshot restore! ",e);
        }
    }

    /**
     * Method invoked when the restore response has been received.
     * It waits that all the responses are received, and after will send the agreed value towards the original handler.
     * It is a synchronized operation.
     * @param restoreSnapshotResponse Message received.
     * @param handler Handler where the message was received from.
     */
    private synchronized void receiveSnapshotRestoreResponse(RestoreSnapshotResponse restoreSnapshotResponse, ClientSocketHandler handler){
        LoggerManager.getInstance().mutableInfo("the node has received a restore response with value: " + restoreSnapshotResponse.isSnapshotValid(), Optional.of(this.getClass().getName()), Optional.of("receiveSnapshotRestoreResponse"));
        if(this.snapshotPendingRequestManager == null) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"No snapshotPendingRequestManager, skipping.");
            return;
        }
        try {
            if(this.snapshotPendingRequestManager.isNodeSnapshotLeader(restoreSnapshotResponse.getSnapshotIdentifier())){
                this.leaderReceiveSnapshotRestoreResponse(restoreSnapshotResponse,handler);
                return;
            }

            if(!restoreSnapshotResponse.isSnapshotValid()){
                LoggerManager.getInstance().mutableInfo("send back negative response", Optional.of(this.getClass().getName()), Optional.of("leaderReceiveSnapshotRestoreResponse"));
                this.snapshotPendingRequestManager.getSnapshotRequestSender(restoreSnapshotResponse.getSnapshotIdentifier()).sendMessage(restoreSnapshotResponse);

                Object lock = this.snapshotPendingRequestManager.getSnapshotLock();
                synchronized(lock){
                    lock.notifyAll();
                }
                return;
            }
            if(this.snapshotPendingRequestManager.removePendingRequest(handler.getRemoteNodeName(),restoreSnapshotResponse.getSnapshotIdentifier())){
                //all pending request has been received
                //or receive an invalid response => I can forward to the leader without waiting for other requests
                LoggerManager.getInstance().mutableInfo("the last pending request has been response, send back result", Optional.of(this.getClass().getName()), Optional.of("leaderReceiveSnapshotRestoreResponse"));
                this.snapshotPendingRequestManager.getSnapshotRequestSender(restoreSnapshotResponse.getSnapshotIdentifier()).sendMessage(restoreSnapshotResponse);
            }

        } catch (SnapshotPendingRequestManagerException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"received inconsistent snapshot response",e);
            //TODO: decide
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE,"Socket closed during snapshot restore! ",e);
        }
    }

    /**
     * Method called only by the leader of the 2PC procedure when restoring a snapshot.
     * It waits for all the response along his branches for the spt, then when everyone responded
     * he can take an decision and send in broadcast the agreed value.
     * It is a synchronized operation.
     * @param restoreSnapshotResponse Message received.
     * @param handler Handler where the message was received from.
     */
    private synchronized void leaderReceiveSnapshotRestoreResponse(RestoreSnapshotResponse restoreSnapshotResponse, ClientSocketHandler handler) {
        LoggerManager.getInstance().mutableInfo("the snapshot leader has received a restore response with value: " + restoreSnapshotResponse.isSnapshotValid(), Optional.of(this.getClass().getName()), Optional.of("leaderReceiveSnapshotRestoreResponse"));
        try {
            RestoreSnapshotRequestAgreementResult result = new RestoreSnapshotRequestAgreementResult(restoreSnapshotResponse);

            if (!restoreSnapshotResponse.isSnapshotValid()){

                this.forwardMessageAlongSPT(result, Optional.empty());

                Object lock = snapshotPendingRequestManager.getSnapshotLock();
                synchronized(lock){
                    lock.notifyAll();
                }
                tryToRestoreSnapshot(restoreSnapshotResponse.getSnapshotIdentifier(),restoreSnapshotResponse.isSnapshotValid());
                this.panicManager.setSnapshotLock(false);
                return;
            }

            if(snapshotPendingRequestManager.removePendingRequest(handler.getRemoteNodeName(),restoreSnapshotResponse.getSnapshotIdentifier())){
                this.forwardMessageAlongSPT(result, Optional.empty());
                tryToRestoreSnapshot(restoreSnapshotResponse.getSnapshotIdentifier(),restoreSnapshotResponse.isSnapshotValid());
                this.panicManager.setSnapshotLock(false);
                if(restoreSnapshotResponse.isSnapshotValid())this.panicManager.setPanicMode(false);
            }

        } catch (SnapshotPendingRequestManagerException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING,"received inconsistent snapshot response",e);
            //TODO: decide
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE,"Socket closed during snapshot restore! ",e);
        }

    }

    /**
     * Method called when a node receives the 2PC agreed value.
     * It will forward the message towards all the other nodes in the spt.
     * It is a synchronized operation.
     * @param agreementResult Message received containing the agreed result.
     * @param handler Handler from where the message was received.
     */
    private synchronized void receiveAgreementResult(RestoreSnapshotRequestAgreementResult agreementResult, ClientSocketHandler handler){
        try {
            this.forwardMessageAlongSPT(agreementResult, Optional.ofNullable(handler));
            this.tryToRestoreSnapshot(agreementResult.getSnapshotIdentifier(), agreementResult.getAgreementResult());
            //unlock panic mode if snapshot restore procedure succeed
            if(agreementResult.getAgreementResult())this.panicManager.setPanicMode(false);
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE,"Socket closed during snapshot restore! ",e);
        }
        //unlockSnapshot
        this.panicManager.setSnapshotLock(false);
        //todo: we need to notify? (in case of negative response)
    }

    /**
     * Method which tries to restore the snapshot locally.
     * It restores the routing table, the direct connections and the spanning tree.
     * @param snapshotIdentifier Unique identifier of the snapshot to be restored.
     * @param result Boolean which contains the response.
     */
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
                return;
                //todo decide
            }catch (SnapshotRestoreException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE,"restoreSnapshot failed",e);
                return;
                //todo decide
            } catch (SocketClosedException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE,"Socket closed during snapshot restore! ",e);
                return;
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
     * It sends the message along all paths of the spanning tree saved.
     * @param msg Message to forward.
     * @param receivedHandler Handler from which the message has been received.
     * @throws SocketClosedException If a socket has been closed due to a crash.
     */
    private void forwardMessageAlongSPT(Message msg, Optional<ClientSocketHandler> receivedHandler) throws SocketClosedException{
        // I can just check the references for simplicity

        // A bit of a nested exception, maybe refactor?
        try {
            ClientSocketHandler anchorHandler = this.spt.getAnchorNodeHandler();
            if (receivedHandler.isEmpty() || receivedHandler.get() != anchorHandler) {
                anchorHandler.sendMessage(msg);
            }
        } catch (SpanningTreeNoAnchorNodeException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Node has no associated handler for this node, do not forward along him", e);
        }

        // Forward along children
        for (ClientSocketHandler h : this.spt.getChildren()) {
            if (receivedHandler.isEmpty() || receivedHandler.get() != h) {
                h.sendMessage(msg);
            }
        }
    }

    /**
     * Method used by the application to send a message towards another node.
     * @param content Serializable object containing the information about the application message.
     * @param destinationNodeName Name of the destination node.
     * @throws DSException If something goes wrong.
     */
    public void sendMessage(Serializable content, NodeName destinationNodeName) throws DSException{
        if(this.panicManager.isLocked()){
            LoggerManager.getInstance().mutableInfo("Panic mode activated, do not send messages anymore...",Optional.of(this.getClass().getName()), Optional.of("sendMessage"));
            throw new DSNetworkCrashedException();
        }
        if(destinationNodeName.equals(this.name)) throw new DSMessageToMyselfException();
        ApplicationMessage message = new ApplicationMessage(content, this.name, destinationNodeName);
        this.forwardMessage(message, destinationNodeName);
    }

    /**
     * Method used to forward a message received towards another node.
     * If the node is not found it routing table it initiates the discovery procedure.
     * @param message Generic message to be forwarded.
     * @param destinationNodeName Name of the destination node.
     * @throws DSException If something goes wrong.
     */
    private void forwardMessage(Message message, NodeName destinationNodeName) throws DSException{
        try {
            ClientSocketHandler handler = this.routingTable.getNextHop(destinationNodeName);
            handler.sendMessage(message);
        } catch (RoutingTableNodeNotPresentException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Node not present in routing table", e);

            // Do discovery. If something goes wrong an exception is thrown.
            this.sendDiscoveryMessage(destinationNodeName);

            // Again a bit of duplicated try-catch statement, maybe refactor
            try {
                ClientSocketHandler handler = this.routingTable.getNextHop(destinationNodeName);
                handler.sendMessage(message);
            } catch (RoutingTableNodeNotPresentException ex) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "We should not be here, the node should be present in the rt", ex);
            } catch (SocketClosedException ex) {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
                this.initiatePanicMode();
                throw new DSConnectionUnavailableException();
            }
        }
        catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            this.initiatePanicMode();
            throw new DSConnectionUnavailableException();
        }
    }

    /**
     * // TODO: fix this runtime exception
     * Method called internally to forward the event through the event system.
     * @param content Callback content to be used.
     */
    private void forwardMessage(CallbackContent content) {
        ApplicationMessage appMessage = (ApplicationMessage) content.getCallBackMessage();
        ThreadPool.submit(()->{
            try {
                this.forwardMessage(appMessage, appMessage.getReceiver());
            } catch (DSException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Method invoked when we need to discover if a node is present in the network.
     * @param destinationNodeName Name of the node to discover.
     * @throws DSException DSNodeUnreachableException, means the node was not found during the discovery process.
     */
    private void sendDiscoveryMessage(NodeName destinationNodeName) throws DSException{
        MessageDiscovery msgd=new MessageDiscovery(this.name, destinationNodeName);

        try{
            this.forwardMessageAlongSPT(msgd, Optional.empty());

            // Do the same as a synchronized message, wait for the reply
            // TODO: a bit of duplicated code
            Object lock = new Object();
            this.ackHandler.insertAckId(msgd.getSequenceNumber(), lock);

            // Wait for a timeout, if ack has been received then all good, else something bad happened.
            synchronized (lock){
                lock.wait(Config.getInt("network.ackTimeout"));
            }

            this.ackHandler.removeAckId(msgd.getSequenceNumber());
        } catch (InterruptedException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "Interrupted exception", e);
            return;
        } catch (AckHandlerAlreadyRemovedException e) {
            // If a runtime exception is thrown it means that the ack has been removed, so it has been received.
            LoggerManager.getInstance().mutableInfo("Ack received, can resume operations...", Optional.of(this.getClass().getName()), Optional.of("sendDiscoveryMessage"));
            return;
        } catch (SocketClosedException e) {
            LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
            this.initiatePanicMode();
            return;
        }
        // If no exception is thrown then it means that the timeout has been ended
        LoggerManager.instanceGetLogger().log(Level.WARNING, "Timeout reached waiting for ack.");

        throw new DSNodeUnreachableException();
    }

    /**
     * Method invoked when a client handler receives a message.
     * to ensure that all the operations in it are atomic on all the structures of the manager.
     * It can distinguish between the normal operating mode and the panic operating mode.
     * It is a synchronized operation.
     * @param m Message received.
     */
    synchronized void receiveMessage(Message m, ClientSocketHandler handler){

        // First of all check if the message needs ack, if it does then send back a message.
        if(m.needsAck()){
            // TODO: need error checking here, and decide what we should do.
            //  This message will be sent asynchronously, so we could also send it in another thread.
            LoggerManager.getInstance().mutableInfo("sending ack require from: "+ m.getClass().getName()+ m.getSequenceNumber(), Optional.of(this.getClass().getName()), Optional.of("receiveMessage"));
            try {
                handler.sendMessage(new MessageAck(m.getSequenceNumber()));
            } catch (SocketClosedException e) {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
                this.initiateCrashProcedure(handler);
            }
        }

        // A message can be received in panic mode only if it is a ping-pong
        // or if it is a join request from an already existing node
        switch(m.getInternalID()){
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
        }

        if(this.panicManager.isLocked()){
            LoggerManager.getInstance().mutableInfo("Panic mode activated, the message received will be lost...",Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return;
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
            case MESSAGE_APP -> {
                ApplicationMessage app = (ApplicationMessage)m;
                if(app.getReceiver().equals(this.name)) {
                    Event messageInputChannel = handler.getMessageInputChannel();
                    messageInputChannel.publish(m);
                }else{
                    this.toForwardEvent.publish(app);
                    // try {
                    //     this.forwardMessage(m,app.getReceiver());
                    // } catch (DSException e) {
                    //     LoggerManager.instanceGetLogger().log(Level.SEVERE, "The message is trying to be routed towards an unreachable node (PS:we should not be here): ", e);
                    // }
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
                    try {
                        handler.sendMessage(msgdr);
                    } catch (SocketClosedException e) {
                        LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
                        this.initiateCrashProcedure(handler);
                    }

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
                    // Again a bit of nested exceptions
                    try {
                        this.forwardMessageAlongSPT(msgd, Optional.of(handler));
                    } catch (SocketClosedException ex) {
                        LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
                        this.initiatePanicMode();
                    }
                } catch (SocketClosedException e) {
                    LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
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

                    // Again a bit of nested exceptions
                    try {
                        this.forwardMessageAlongSPT(msgdr, Optional.of(handler));
                    } catch (SocketClosedException ex) {
                        LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
                        this.initiatePanicMode();
                    }
                } catch (SocketClosedException e) {
                    LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been closed. It is not possible to send messages anymore.", e);
                }
            }
            case MESSAGE_NODE_CRASHED -> {
                this.initiatePanicMode();
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

    /**
     * Getter of the internal routing table.
     * @return The routing table.
     */
    // <editor-fold desc="Static Getter">
    public RoutingTable getRoutingTable(){
        return routingTable;
    }

    /**
     * Getter of the internal spanning tree.
     * @return The spanning tree.
     */
    public SpanningTree getSpt(){
        return this.spt;
    }
    // </editor-fold>
}
