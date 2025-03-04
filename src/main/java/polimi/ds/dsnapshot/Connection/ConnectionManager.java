package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.Exit.ExitMsg;
import polimi.ds.dsnapshot.Connection.Messages.Exit.ExitNotify;
import polimi.ds.dsnapshot.Connection.Messages.Join.DirectConnectionMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinForwardMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinMsg;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageAck;
import polimi.ds.dsnapshot.Exception.RoutingTableException;
import polimi.ds.dsnapshot.Exception.SpanningTreeException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;


/**
 * The connection manager consists of a TCP server socket who can receive connections.
 * When a new connection is received a channel is opened with which we can exchange messages.
 */
public class ConnectionManager {

    /**
     * List of active connections
     */
    private List<ClientSocketHandler> handlerList;

    private final AtomicReference<RoutingTable> routingTable = new AtomicReference<>();
    private final AtomicReference<SpanningTree> spt = new AtomicReference<>();
    /**
     * Reference to the handler of the acks
     */
    private final AckHandler ackHandler;
    /**
     * Port of the server
     */
    private final int port;
    //TODO: put it in a constant ?
    double directConnectionProbability = 0.7; // 70%

    /**
     * Mute attribute of the manager
     */
    private boolean mute=false;

    /**
     * Constructor of the connection manager
     */
    public ConnectionManager(int port){
        this.handlerList = new ArrayList<>();
        this.ackHandler = new AckHandler();
        this.port = port;

        System.out.println("[ConnectionManager] ConnectionManager created successfully...");
    }

    /**
     * Constructor of the connection manager
     * @param mute specify if the manager is muted or not
     */
    public ConnectionManager(int port, boolean mute){
        this(port);
        this.mute=mute;
    }

    public void start(){
        if(!this.mute) System.out.println("[ConnectionManager] Preparing the thread...");

        // This start has to launch another thread.

        Thread t = new Thread(()->{

            try(ServerSocket serverSocket = new ServerSocket(this.port)){
                if(!this.mute) System.out.println("[ConnectionManager] Created listening socket on port "+this.port+" ...");

                try(ExecutorService executor = Executors.newCachedThreadPool()){
                    if(!this.mute) System.out.println("[ConnectionManager] Created thread pool...");

                    while(true){
                        if(!this.mute) System.out.println("[ConnectionManager] Waiting for connection...");
                        Socket socket = serverSocket.accept();
                        if(!this.mute) System.out.println("[ConnectionManager] Accepted connection from " + socket.getRemoteSocketAddress()+" ...");
                        ClientSocketHandler handler = new ClientSocketHandler(socket, this);
                        this.handlerList.add(handler);
                        executor.submit(handler);
                        if(!this.mute) System.out.println("[ConnectionManager] Connection submitted to executor...");
                    }

                }catch (RuntimeException e){
                    // TODO: what to do ?
                    System.err.println("[ConnectionManager] Runtime exception: "+e.getMessage());
                }

            }catch (IOException e){
                System.err.println("[ConnectionManager] IO exception: " + e.getMessage());
                // TODO: what to do ?
            }
            // Here the serverSocket is closed
            if(!this.mute) System.out.println("[ConnectionManager] Shutting down...");
        });

        if(!mute) System.out.println("[ConnectionManager] Launching the thread...");

        t.start();
    }

    // TODO: maybe its better if the method is private (called by a generic sendMessage that works as interface)
    // TODO: refactor well to work with exceptions
    // TODO: discuss a bit if every message needs the destination ip:port
    // TODO: there is a problem, the MessageAck is a different class than the Message
    public boolean sendMessageSynchronized(Message m, String ip, int port){

        if(!this.mute) System.out.println("[ConnectionManager] Sending a message to "+ip+":"+port+"...");

        NetNode destNode = new NetNode(ip, port);

        try {
            if(!this.mute) System.out.println("[ConnectionManager] Checking the routing table for the next hop...");
            ClientSocketHandler handler = routingTable.get().getNextHop(destNode);

            return this.sendMessageSynchronized(m,handler);
        } catch (RoutingTableException e) {
            return false;
        }
    }

    private boolean sendMessageSynchronized(Message m, ClientSocketHandler handler){
        if(!this.mute) System.out.println("[ConnectionManager] Preparing for receiving an ack...");
        int seqn = m.getSequenceNumber();
        this.ackHandler.insertAckId(seqn);

        if(!this.mute) System.out.println("[ConnectionManager] Sending the message ...");
        boolean b = handler.sendMessage(m);


        if(!b) {
            if(!this.mute) System.out.println("[ConnectionManager] Something went wrong while sending the message...");
            return false;
        }

        if(!this.mute) System.out.println("[ConnectionManager] Sent, now waiting for ack...");

        // Here I need to synchronize
        // TODO: this is basically a spinlock, other option is to go to sleep
        // TODO: also gives problems when the ack is not received -> wait forever ? lol i dont think so
        while(this.ackHandler.isAckIdPresent(seqn));

        // Here some other thread will have removed the sequence number from the set so it means that the ack
        // Has been received correctly, and it is safe to return

        if(!this.mute) System.out.println("[ConnectionManager] Ack received, can resume operations...");

        return true;
    }

    private void sendBroadcastMsg(Message msg){
        for(ClientSocketHandler h : spt.get().getChildren()) {
            h.sendMessage(msg);
        }
        ClientSocketHandler anchorNodeHandler = spt.get().getAnchorNodeHandler();
        if(anchorNodeHandler != null)anchorNodeHandler.sendMessage(msg);
    }


    /**
     * Retrieves the IP address of the local machine.
     *  @return a character array (`char[]`) representing the local machine's IP address.
     *  @throws UnknownHostException if the machine's IP address cannot be determined.
     */
    // TODO: maybe do not need synchronized? If we assume the IP does not change...
    private synchronized char[] getMachineIp() throws UnknownHostException {
        // Get the local host address
        InetAddress localHost = InetAddress.getLocalHost();

        // Get the IP address as a string
        String ipAddress = localHost.getHostAddress();
        return ipAddress.toCharArray();
    }

    // <editor-fold desc="Join procedure">
    /**
     * Establishes a connection to an anchor node in the network by creating a socket connection.
     * Sends a `JoinMsg` message to the specified node in order to initiate the join process.

     * @param anchorIp   a character array representing the IP address of the node to join
     * @param anchorPort the port number of the node to join
     * @throws IOException if an I/O error occurs during socket connection or communication
     */
    public synchronized void joinNet(String anchorIp, int anchorPort) throws IOException {
        JoinMsg msg = new JoinMsg(Arrays.toString(getMachineIp()), this.port);
        //create socket for the anchor node, add to direct connection list and save as anchor node
        ClientSocketHandler handler = new ClientSocketHandler(new Socket(anchorIp,anchorPort, mute), this);
        handler.run();
        handlerList.add(handler);
        //send join msg to anchor node & wait for ack
        this.sendMessageSynchronized(msg,handler);
        //handler.sendMessage(msg);
        spt.get().setAnchorNodeHandler(handler);
        //TODO wait for ack and add handler to routing table when receive ack and start ping pong
    }

    /**
    * Handles a join request received from another node in the network.
    *
    * @param msg     the {@link JoinMsg} containing information about the joining node.
    * @param handler the {@link ClientSocketHandler} managing the client communication
    *                for the incoming connection.
    * @throws UnknownHostException if the IP address of the host node cannot be resolved.
    */
    private void receiveJoin(JoinMsg msg, ClientSocketHandler handler) throws UnknownHostException {
        try {
            //add node in direct connection list and in routing table
            receiveDirectConnectionMessage((DirectConnectionMsg) msg, handler);
            spt.get().addChild(handler);//TODO: maybe is better if add child when receive first ping
        } catch (RoutingTableException | SpanningTreeException e) {
            //TODO manage: if I receive a join from a node already in the routing table (wtf)
            return;
        }

        //forward join notify to neighbour

        JoinForwardMsg m = new JoinForwardMsg(msg.getIp(),msg.getPort(), Arrays.toString(this.getMachineIp()),this.port);

        for(ClientSocketHandler h : this.handlerList){
            if(h!=handler)h.sendMessage(m);
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
                //create socket connection with the joiner to instantiate a new direct connection
                ClientSocketHandler joinerHandler = new ClientSocketHandler(new Socket(msg.getIp(),msg.getPort(), mute), this);
                joinerHandler.run();
                //send to joiner a message to create a direct connection
                joinerHandler.sendMessage(new DirectConnectionMsg(Arrays.toString(this.getMachineIp()),this.port));
                //save the direct connection in the handler list
                handlerList.add(joinerHandler);
                //add node in routing table
                routingTable.get().addPath(new NetNode(msg.getIp(), msg.getPort()), joinerHandler);
            }else {
                //creating undirected path to the joiner node with the anchor node
                routingTable.get().addPath(new NetNode(msg.getAnchorIp(), msg.getAnchorPort()),handler);
            }
        } catch (RoutingTableException e) {
            return;
            //TODO manage: if I receive a join forward from a node already in the routing table (wtf)
        }
    }
    /**
     * Processes a direct connection message received from another node in the network.
     * @param msg the {@link DirectConnectionMsg} containing the details of the direct connection request.
     * @param handler the {@link ClientSocketHandler} managing the communication context for the incoming message.
     * @throws RoutingTableException if the ip address is already in the {@link RoutingTable}
     */
    private void receiveDirectConnectionMessage(DirectConnectionMsg msg, ClientSocketHandler handler) throws RoutingTableException {
        //add node in routing table
        routingTable.get().addPath(new NetNode(msg.getIp(), msg.getPort()), handler);
        //save the direct connection in the handler list
        handlerList.add(handler);
    }
    // </editor-fold>

    // <editor-fold desc="Exit procedure">
    public synchronized void exitNet() throws IOException {
        //reassign all child to the current anchor node of the exiting node
        ClientSocketHandler handler = spt.get().getAnchorNodeHandler();

        //send exit message to all child
        ExitMsg m = new ExitMsg(handler.getRemoteIp(),handler.getRemotePort());
        sendBroadcastMsg(m);

        //clear handler list
        handlerList = new ArrayList<>();

        //clear spt
        spt.set(new SpanningTree());

        //clear routing table
        routingTable.get().clearTable();
    }

    private void receiveExit(ExitMsg msg, ClientSocketHandler handler) throws IOException {
        try {
            routingTable.get().removePath(new NetNode(handler.getRemoteIp(),handler.getRemotePort()));
            routingTable.get().removeAllIndirectPath(handler);
            handler.close();
            handlerList.remove(handler);

            ClientSocketHandler anchorNodeHandler = spt.get().getAnchorNodeHandler();
            if(handler == anchorNodeHandler){
                //reassign anchor node
                spt.get().setAnchorNodeHandler(null);
                this.sendExitNotify(handler.getRemoteIp(), handler.getRemotePort());
                this.newAnchorNode(msg);
            }else if(anchorNodeHandler != null){
                //forward exit notify to anchor node only
                this.sendExitNotify(handler.getRemoteIp(), handler.getRemotePort());

                //TODO send to anchor node only isn't enough, discuss how to avoid message loops
            }

        } catch (Exception e) {
            //TODO if ip not in routing table
            return;
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
            newAnchorNextHop = routingTable.get().getNextHop(new NetNode(msg.getNewAnchorIp(), msg.getNewAnchorPort()));
        } catch (RoutingTableException e) {
            // No path to reach the new anchor node, establish a direct connection.
            this.joinNet(msg.getNewAnchorIp(), msg.getNewAnchorPort());
            return;
        }

        // Check if there is already a direct connection with the new anchor node.
        if(Objects.equals(newAnchorNextHop.getRemoteIp(), msg.getNewAnchorIp()) && newAnchorNextHop.getRemotePort()==msg.getNewAnchorPort()){
            //TODO: there is already a direct cnt between this node and the anchor -> start ping pong
            //set new anchor node
            spt.get().setAnchorNodeHandler(newAnchorNextHop);
            return;
        }
        // No direct connection with the new anchor node; establish one.
        this.joinNet(msg.getNewAnchorIp(), msg.getNewAnchorPort());
    }
    private void sendExitNotify(String exitIp, int exitPort){
        ExitNotify exitNotify = new ExitNotify(exitIp, exitPort);
        sendBroadcastMsg(exitNotify);
    }

    // </editor-fold>
    // TODO: add the send of a message via the routing table

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
            handler.sendMessage(new MessageAck(m.getSequenceNumber()));
        }

        // Switch the ID of the message and do what you need to do:
        // TODO: I have an idea to possibly be more efficient.
        //  Maybe not all messages need a full locking on the object so you can pass it in the internal bits

        switch(m.getInternalID()){
            case MESSAGE_JOIN -> {
                try {
                    this.receiveJoin((JoinMsg) m, handler);
                } catch (UnknownHostException e) {
                    // TODO: decide
                    System.err.println("[ConnectionManager] Unknown host: " + e.getMessage());
                }
            }
            case MESSAGE_EXIT -> {
                try {
                    this.receiveExit((ExitMsg) m, handler);
                } catch (IOException e) {
                    // TODO: decide
                    System.err.println("[ConnectionManager] IO exception: " + e.getMessage());
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
                    System.err.println("[ConnectionManager] IO exception: " + e.getMessage());
                }
            }
            case MESSAGE_DIRECTCONNECTION -> {
                try{
                    this.receiveDirectConnectionMessage((DirectConnectionMsg) m, handler);
                }
                catch (RoutingTableException e){
                    // TODO: decide, i dont know what these exceptions do
                    System.err.println("[ConnectionManager] Routing table exception: " + e.getMessage());
                }
                break;
            }
            case MESSAGE_ACK -> {
                // If the message received is an ack then remove it from the ack handler
                this.ackHandler.removeAckId(m.getSequenceNumber());
            }
            case MESSAGE_NOTIMPLEMENTED -> {
                // TODO: decide, should be the same as default
                break;
            }
            case null, default -> {
                // TODO: decide
            }
        }

    }

}
