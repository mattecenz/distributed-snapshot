package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.DirectConnectionMsg;
import polimi.ds.dsnapshot.Connection.Messages.JoinForwardMsg;
import polimi.ds.dsnapshot.Connection.Messages.JoinMsg;
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


    // <editor-fold desc="Join procedure">
    /**
     * Establishes a connection to an anchor node in the network by creating a socket connection.
     * Sends a `JoinMsg` message to the specified node in order to initiate the join process.

     * @param anchorIp   a character array representing the IP address of the node to join
     * @param anchorPort the port number of the node to join
     * @throws IOException if an I/O error occurs during socket connection or communication
     */
    private void JoinNet(char [] anchorIp, int anchorPort) throws IOException {
        JoinMsg msg = new JoinMsg(getMachineIp(), this.port);
        ClientSocketHandler handler = new ClientSocketHandler(new Socket(Arrays.toString(anchorIp),anchorPort, mute));
        handler.run();
        handlerList.add(handler);

        handler.sendMessage(msg);
        //TODO wait for ack and add handler to routing table when receive ack and start ping pong
    }

    private char[] getMachineIp() throws UnknownHostException {
        // Get the local host address
        InetAddress localHost = InetAddress.getLocalHost();

        // Get the IP address as a string
        String ipAddress = localHost.getHostAddress();
        return ipAddress.toCharArray();
    }

    private void ReceiveJoin(JoinMsg msg, ClientSocketHandler handler) throws UnknownHostException {
        try {
            receiveDirectConnectionMessage((DirectConnectionMsg) msg, handler);
        } catch (RoutingTableException e) {
            //TODO manage: if I receive a join from a node already in the routing table (wtf)
            return;
        }
        JoinForwardMsg m = new JoinForwardMsg(msg.getIp(),msg.getPort(),this.getMachineIp(),this.port);

        for(ClientSocketHandler h : this.handlerList){
            h.sendMessage(m);
        }

    }

    private void ReceiveJoinForward(JoinForwardMsg msg, ClientSocketHandler handler) throws IOException {
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

                //TODO create direct connection
            }else {
                //creating undirected path to the joiner node with the anchor node
                routingTable.get().addPath(new NetNode(msg.getAnchorIp(), msg.getAnchorPort()),handler);
            }
        } catch (RoutingTableException e) {
            return;
            //TODO manage: if I receive a join forward from a node already in the routing table (wtf)
        }
    }

    private void receiveDirectConnectionMessage(DirectConnectionMsg msg, ClientSocketHandler handler) throws RoutingTableException {
        //add node in routing table
        routingTable.get().addPath(new NetNode(msg.getIp(), msg.getPort()), handler);
        //save the direct connection in the handler list
        handlerList.add(handler);
    }
    // </editor-fold>



    // TODO: add the send of a message via the routing table

}
