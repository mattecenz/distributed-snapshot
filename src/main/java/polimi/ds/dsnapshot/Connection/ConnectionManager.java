package polimi.ds.dsnapshot.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * The connection manager consists of a TCP server socket who can receive connections.
 * When a new connection is received a channel is opened with which we can exchange messages.
 */
class ConnectionManager {

    /**
     * List of active connections
     */
    private final List<ClientSocketHandler> handlerList;

    /**
     * Port of the server
     */
    private final int port;

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

    // TODO: add the send of a message via the routing table

}
