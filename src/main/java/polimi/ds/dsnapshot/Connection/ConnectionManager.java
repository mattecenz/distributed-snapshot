package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.Exit.ExitMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.DirectConnectionMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinForwardMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinMsg;
import polimi.ds.dsnapshot.Exception.RoutingTableException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private final List<ClientSocketHandler> handlerList;
    private ClientSocketHandler anchorNodeHandler;
    private final AtomicReference<RoutingTable> routingTable = new AtomicReference<>();
    /**
     * Port of the server
     */
    private final int port;
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
                        ClientSocketHandler handler = new ClientSocketHandler(socket);
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

    /**
     * Retrieves the IP address of the local machine.
     *  @return a character array (`char[]`) representing the local machine's IP address.
     *  @throws UnknownHostException if the machine's IP address cannot be determined.
     */
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
    public synchronized void JoinNet(char [] anchorIp, int anchorPort) throws IOException {
        JoinMsg msg = new JoinMsg(getMachineIp(), this.port);
        //create socket for the anchor node, add to direct connection list and save as anchor node
        ClientSocketHandler handler = new ClientSocketHandler(new Socket(Arrays.toString(anchorIp),anchorPort, mute));
        handler.run();
        handlerList.add(handler);
        anchorNodeHandler = handler;
        //send join msg to anchor node
        handler.sendMessage(msg);
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
    private synchronized void ReceiveJoin(JoinMsg msg, ClientSocketHandler handler) throws UnknownHostException {
        try {
            //add node in direct connection list and in routing table
            receiveDirectConnectionMessage((DirectConnectionMsg) msg, handler);
        } catch (RoutingTableException e) {
            //TODO manage: if I receive a join from a node already in the routing table (wtf)
            return;
        }

        //forward join notify to neighbour
        JoinForwardMsg m = new JoinForwardMsg(msg.getIp(),msg.getPort(),this.getMachineIp(),this.port);

        for(ClientSocketHandler h : this.handlerList){
            h.sendMessage(m);
        }

    }
    /**
     * Handles a forwarded join request in the network.
     * @param msg the {@link JoinForwardMsg} containing details about the forwarder and the joiner.
     * @param handler the {@link ClientSocketHandler} managing the client communication
     */
    private synchronized void ReceiveJoinForward(JoinForwardMsg msg, ClientSocketHandler handler) throws IOException {
        double randomValue = ThreadLocalRandom.current().nextDouble();

        try {
            if(randomValue < this.directConnectionProbability){
                //create socket connection with the joiner to instantiate a new direct connection
                ClientSocketHandler joinerHandler = new ClientSocketHandler(new Socket(Arrays.toString(msg.getIp()),msg.getPort(), mute));
                joinerHandler.run();
                //send to joiner a message to create a direct connection
                joinerHandler.sendMessage(new DirectConnectionMsg(this.getMachineIp(),this.port));
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
    private synchronized void receiveDirectConnectionMessage(DirectConnectionMsg msg, ClientSocketHandler handler) throws RoutingTableException {
        //add node in routing table
        routingTable.get().addPath(new NetNode(msg.getIp(), msg.getPort()), handler);
        //save the direct connection in the handler list
        handlerList.add(handler);
    }
    // </editor-fold>

    // <editor-fold desc="Exit procedure">
    public synchronized void ExitNet() throws IOException {
        if (handlerList.isEmpty()) return;
        //select randomly 1 direct connection
        int randomIndex = ThreadLocalRandom.current().nextInt(handlerList.size());
        ClientSocketHandler handler = handlerList.get(randomIndex);

        //send exit message with info on the random selected connection and close socket
        ExitMsg m = new ExitMsg(handler.getRemoteIp().toCharArray(),handler.getRemotePort());
        for(ClientSocketHandler h : this.handlerList){
            h.sendMessage(m);
            h.close();
            handlerList.remove(h);
        }
        //clear routing table
        routingTable.get().clearTable();
    }

    private synchronized void ReceiveExit(ExitMsg msg, ClientSocketHandler handler) throws IOException {
        try {
            routingTable.get().removePath(new NetNode(handler.getRemoteIp().toCharArray(),handler.getRemotePort()));
            routingTable.get().removeAllIndirectPath(handler);
            handler.close();
            handlerList.remove(handler);

            //TODO manage new anchor
            //TODO send exit notify

        } catch (Exception e) {
            //TODO if ip not in routing table
            return;
        }

    }

    // </editor-fold>

    // TODO: add the send of a message via the routing table

}
