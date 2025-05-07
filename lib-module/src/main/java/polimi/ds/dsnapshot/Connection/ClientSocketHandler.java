package polimi.ds.dsnapshot.Connection;

import org.jetbrains.annotations.TestOnly;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Api.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Exception.SocketClosedException;
import polimi.ds.dsnapshot.Exception.SocketCrashedException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Handler of the socket connection between two nodes.
 * It is a Runnable object so it can be launched in a separate thread.
 */
public class ClientSocketHandler implements Runnable{
    /**
     * Socket which represents the connection.
     */
    private final Socket socket;
    /**
     * Output stream.
     */
    private ObjectOutputStream out;

    /**
     * Input stream.
     */
    private ObjectInputStream in;
    /**
     * Channel used to forward application message to application layer.
     */
    private Event messageInputChannel;
     /**
     * Ping pong manager associated to the handler.
     */
     private PingPongManager pingPongManager;
    /**
     * Name of the remote client associated to this socket.
     */
    private final NodeName remoteNodeName;
    /**
     * Boolean which indicates if the connection was initiated in this node or in the other node.
     */
    private final boolean isOwner;
    /**
     * Reference to the original connection manager for callback when a message is received.
     */
    private final ConnectionManager manager;

    /**
     * Shared variable used for checking if the server is still listening or not.
     */
    private final AtomicBoolean inAvailable;

    /**
     * Lock used for output (send) operations.
     */
    private final Object outLock;

    /**
     * Constructor of the handler.
     * @param socket Socket to be managed.
     * @param remoteNodeName Name of the remote node.
     * @param manager Reference to the connection manager.
     * @param isOwner If the node is the first to open the connection.
     */
    public ClientSocketHandler(Socket socket, NodeName remoteNodeName, ConnectionManager manager, boolean isOwner) {
        this.socket = socket;
        this.remoteNodeName = remoteNodeName;
        this.inAvailable = new AtomicBoolean(false);
        this.manager = manager;
        this.outLock = new Object();
        this.setOutStream();
        this.isOwner = isOwner;

        this.prepareMessageInputEvent(JavaDistributedSnapshot.getInstance());

        LoggerManager.getInstance().mutableInfo("Socket connected at address:" + socket.getInetAddress() + ":" + socket.getPort(), Optional.of(this.getClass().getName()), Optional.of("ClientSocketHandler"));
    }

    /**
     * Constructor of the handler form a previous unnamed socket handler.
     * @param unhandler UnNamedSocketHandler constructed before.
     * @param remoteNodeName Name of the remote node.
     * @param manager Reference to the connection manager.
     */
    public ClientSocketHandler(UnNamedSocketHandler unhandler, NodeName remoteNodeName, ConnectionManager manager){
        this.socket = unhandler.getSocket();
        this.remoteNodeName = remoteNodeName;
        // The input stream is retrieved from the socket handler before
        this.inAvailable = new AtomicBoolean(true);
        this.manager = manager;
        this.isOwner = false;

        this.outLock = new Object();

        // This is the fundamental difference between the constructors
        this.in = unhandler.getIn();
        this.setOutStream();

        this.prepareMessageInputEvent(JavaDistributedSnapshot.getInstance());

        LoggerManager.getInstance().mutableInfo("Socket connected at address:" + socket.getInetAddress() + ":" + socket.getPort(), Optional.of(this.getClass().getName()), Optional.of("ClientSocketHandler"));
    }

    /**
     * Constructor of the handler without the connection manager.
     * Used only for testing purposes.
     * @param socket Socket to be managed.
     * @param remoteNodeName Name of the remote node.
     */
    @TestOnly
    public ClientSocketHandler(Socket socket, NodeName remoteNodeName) {
        this(socket, remoteNodeName, null, false);
    }

    /**
     * Check if the node is the owner of the handler.
     * @return True if this node is the owner.
     */
    public boolean isNodeOwner() {
        return isOwner;
    }

    /**
     * Method to initiate the output stream of the socket.
     */
    private void setOutStream(){
        try {
            LoggerManager.getInstance().mutableInfo("Creating the output stream...", Optional.of(this.getClass().getName()), Optional.of("setStream"));
            this.out = new ObjectOutputStream(this.socket.getOutputStream());
        }catch (IOException e){
            //TODO: what to do ?
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
        }
        LoggerManager.getInstance().mutableInfo("Stream ready", Optional.of(this.getClass().getName()), Optional.of("setStream"));
    }


    /**
     * Method to prepare the input event channel used for saving messages when the snapshot is active.
     * @param javaDistributedSnapshot Reference to the snapshot instance which contains the method to receive messages
     *                                to bind to the event system.
     */
    private void prepareMessageInputEvent(JavaDistributedSnapshot javaDistributedSnapshot){
        try {
            messageInputChannel = EventsBroker.createEventChannel(this.remoteNodeName.getIP()+":"+this.remoteNodeName.getPort());
        } catch (EventException e) {
            //todo decide
            throw new RuntimeException(e);
        }
        messageInputChannel.subscribe(javaDistributedSnapshot::ReceiveMessage);
    }

    /**
     * Getter of the input event channel.
     * @return The input event channel.
     */
    public Event getMessageInputChannel() {
        return messageInputChannel;
    }

    /**
     * Method launched when the thread is submitted.
     * It stays listening for new messages received on the input channel.
     */
    @Override
    public void run() {
        // Create the input stream
        if(!this.inAvailable.get()) {
            LoggerManager.getInstance().mutableInfo("Creating input stream..", Optional.of(this.getClass().getName()), Optional.of("setStream"));
            try {
                this.in = new ObjectInputStream(this.socket.getInputStream());
                this.inAvailable.set(true);
            } catch (IOException e) {
                // Not much we can do, it is outside our control this error.
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                // Use this only as debug.
                System.err.println("IO exception: "+e.getMessage());
            }
            // Now we are ready to listen incoming messages

        }

        // Read a generic message and decide what to do
        while(this.inAvailable.get()){
            try {
                LoggerManager.getInstance().mutableInfo("Listening..", Optional.of(this.getClass().getName()+this.hashCode()), Optional.of("run"));
                Message m = (Message) this.in.readObject();
                LoggerManager.getInstance().mutableInfo("Message received: " +m.getClass().getName(), Optional.of(this.getClass().getName()+this.hashCode()), Optional.of("run"));
                // I guess just pass the message to the ConnectionManager ? A bit ugly but it works.
                this.manager.receiveMessage(m, this);
            } catch (EOFException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "The channel has been forcefully closed from the other side.", e);
                this.inAvailable.set(false);
                // Initiate crash procedure
                this.manager.initiateCrashProcedure(this);
                // The socket will be closed
            }catch (ClassNotFoundException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "ClassNotFoundException", e);
                // Not much we can do, outside of our control.
                // Use this only as debug.
                System.err.println("IO exception: "+e.getMessage());
                this.inAvailable.set(false);
            }catch (NullPointerException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "NullPointerException", e);
                // Not much we can do, outside of our control.
                // Use this only as debug.
                System.err.println("IO exception: "+e.getMessage());
                this.inAvailable.set(false);
            } catch (IOException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO Exception", e);
                // Not much we can do, outside of our control.
                // Use this only as debug.
                System.err.println("IO exception: "+e.getMessage());
                this.inAvailable.set(false);
            }
        }
    }

    /**
     * Method to gracefully close the socket connection.
     */
    public void close() {
        try {
            this.socket.close();
            LoggerManager.getInstance().mutableInfo("Socket closed!", Optional.of(this.getClass().getName()), Optional.of("close"));
        } catch (IOException e) {
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO Exception", e);
            // Not much we can do, outside of our control.
            // Use this only as debug.
            System.err.println("IO exception: "+e.getMessage());
        }
        this.inAvailable.set(false);
        try {
            EventsBroker.removeEventChannel(this.remoteNodeName.getIP()+":"+this.remoteNodeName.getPort());
        } catch (EventException e) {
            // Do nothing
            LoggerManager.instanceGetLogger().log(Level.WARNING, "Event exception: ", e);
        }
    }

    /**
     * Method invoked when sending a message to the output stream.
     * This method is thread safe.
     * @param m Message to be sent.
     * @throws SocketClosedException If the socket you are trying to send the message with is closed.
     */
    public void sendMessage(Message m) throws SocketClosedException {
        synchronized (this.outLock) {
            LoggerManager.getInstance().mutableInfo("Lock acquired...", Optional.of(this.getClass().getName()), Optional.of("sendMessage"));

            try {
                LoggerManager.getInstance().mutableInfo("Sending message: " + m.getClass().getName() + " to: "+remoteNodeName.getIP() +": " + remoteNodeName.getPort(), Optional.of(this.getClass().getName()), Optional.of("sendMessage"));
                // Important. synchronize everything in the output stream
                this.out.writeObject(m);
                this.out.flush();
            } catch (SocketException e) {
                LoggerManager.instanceGetLogger().log(Level.WARNING, "The socket has been abruptly closed. Cannot send messages anymore", e);
                // Launch the exception
                throw new SocketClosedException();
            } catch (IOException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                // Not much we can do, outside of our control.
                // Use this only as debug.
                System.err.println("IO exception: "+e.getMessage());
            }
        }
    }

    /**
     * Get the name of the remote socket
     * @return the name of the remote socket
     */
    public NodeName getRemoteNodeName(){
        return this.remoteNodeName;
    }

    /**
     * Method to create a new ping pong manager and start the ping pong procedure.
     * @param isFirstPing true if the ping received is the first.
     */
    protected void startPingPong(boolean isFirstPing){
        pingPongManager = new PingPongManager(manager,this, isFirstPing);
    }

    /**
     * Method to stop the ping pong procedure.
     */
    protected void stopPingPong(){
        pingPongManager.stopPingPong();
    }


}
