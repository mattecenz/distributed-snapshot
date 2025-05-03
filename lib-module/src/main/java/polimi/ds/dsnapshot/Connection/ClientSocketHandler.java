package polimi.ds.dsnapshot.Connection;

import org.jetbrains.annotations.TestOnly;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Api.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class ClientSocketHandler implements Runnable{
    /**
     * Socket which represents the connection
     */
    private final Socket socket;
    /**
     * Output stream
     */
    private ObjectOutputStream out;

    /**
     * Input stream
     */
    private ObjectInputStream in;
    /**
     * channel used to forward application message to application layer
     */
    private Event messageInputChannel;
     /**
     * ping pong
     */
     private PingPongManager pingPongManager;
    /**
     * Name of the remote client associated to this socket.
     * Careful, since it is not final it may happen that there is a period that the client remains with no name
     */
    private final NodeName remoteNodeName;

    private final boolean isOwner;
    /**
     * Reference to the original connection manager for callback when a message is received
     */
    private final ConnectionManager manager;

    /**
     * Boolean to check  if the socket output handler is ready
     */
    private final AtomicBoolean outAvailable;

    /**
     * Shared variable used for checking if the server is still listening or not
     */
    private final AtomicBoolean inAvailable;

    /**
     * Lock used for output (send) operations
     */
    private final Object outLock;

    /**
     * Constructor of the handler
     * @param socket socket to be managed
     * @param remoteNodeName name of the remote node
     * @param manager reference to the connection manager
     * @param isOwner if the node is the first to open the connection
     */
    public ClientSocketHandler(Socket socket, NodeName remoteNodeName, ConnectionManager manager, boolean isOwner) {
        this.socket = socket;
        this.remoteNodeName = remoteNodeName;
        this.outAvailable = new AtomicBoolean(false);
        this.inAvailable = new AtomicBoolean(false);
        this.manager = manager;
        this.outLock = new Object();
        this.setOutStream();
        this.isOwner = isOwner;

        this.prepareMessageInputEvent(JavaDistributedSnapshot.getInstance());

        LoggerManager.getInstance().mutableInfo("Socket connected at address:" + socket.getInetAddress() + ":" + socket.getPort(), Optional.of(this.getClass().getName()), Optional.of("ClientSocketHandler"));
    }

    /**
     * Constructor of the handler form a previous unnamed socket handler
     * @param unhandler UnNamedSocketHandler constructed before
     * @param remoteNodeName name of the remote node
     * @param manager reference to the connection manager
     */
    public ClientSocketHandler(UnNamedSocketHandler unhandler, NodeName remoteNodeName, ConnectionManager manager){
        this.socket = unhandler.getSocket();
        this.remoteNodeName = remoteNodeName;
        this.outAvailable = new AtomicBoolean(false);
        // The input stream is retrieved from the socket handler before
        this.inAvailable = new AtomicBoolean(true);
        this.manager = manager;
        this.isOwner = false;

        this.outLock = new Object();

        // This is the fundamental difference
        this.in = unhandler.getIn();
        this.setOutStream();

        this.prepareMessageInputEvent(JavaDistributedSnapshot.getInstance());

        LoggerManager.getInstance().mutableInfo("Socket connected at address:" + socket.getInetAddress() + ":" + socket.getPort(), Optional.of(this.getClass().getName()), Optional.of("ClientSocketHandler"));
    }

    /**
     * Constructor of the handler without the connection manager.
     * USE ONLY FOR TESTING !!!!!!!!!!!!!!!!!!!!!!
     * @param socket socket to be managed
     * @param remoteNodeName name of the remote node
     */
    @TestOnly
    public ClientSocketHandler(Socket socket, NodeName remoteNodeName) {
        this(socket, remoteNodeName, null, false);
    }

    public boolean isNodeOwner() {
        return isOwner;
    }

    private void setOutStream(){
        try {
            LoggerManager.getInstance().mutableInfo("Creating the output stream...", Optional.of(this.getClass().getName()), Optional.of("setStream"));
            this.out = new ObjectOutputStream(this.socket.getOutputStream());
            this.outAvailable.set(true);
        }catch (IOException e){
            //TODO: what to do ?
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
        }
        LoggerManager.getInstance().mutableInfo("Stream ready", Optional.of(this.getClass().getName()), Optional.of("setStream"));
    }



    private void prepareMessageInputEvent(JavaDistributedSnapshot javaDistributedSnapshot){
        try {
            messageInputChannel = EventsBroker.createEventChannel(this.remoteNodeName.getIP()+":"+this.remoteNodeName.getPort());
        } catch (EventException e) {
            //todo decide
            throw new RuntimeException(e);
        }
        messageInputChannel.subscribe(javaDistributedSnapshot::ReceiveMessage);
    }

    public Event getMessageInputChannel() {
        return messageInputChannel;
    }

    /**
     * Method launched when the thread is submitted
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
                //TODO: what to do ?
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
            }
            // Now we are ready to listen incoming messages

        }

        // Read a generic message and decide what to do
        while(inAvailable.get()){
            try {
                LoggerManager.getInstance().mutableInfo("Listening..", Optional.of(this.getClass().getName()+this.hashCode()), Optional.of("run"));
                Message m = (Message) this.in.readObject();
                LoggerManager.getInstance().mutableInfo("Message received: " +m.getClass().getName(), Optional.of(this.getClass().getName()+this.hashCode()), Optional.of("run"));
                // I guess just pass the message to the ConnectionManager ? A bit ugly but it works.
                this.manager.receiveMessage(m, this);
            } catch (IOException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                // TODO: what to do ? <- here if not receive msg for 2*pingPongTimeout
                this.inAvailable.set(false);
            }catch (ClassNotFoundException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "ClassNotFoundException", e);
                // TODO: what to do ?
                this.inAvailable.set(false);
            }catch (NullPointerException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "NullPointerException", e);
                System.out.println(e.getMessage());
                this.inAvailable.set(false);
            }
        }
    }

    public void close() throws IOException {
        socket.close();
        LoggerManager.getInstance().mutableInfo("Socket closed!", Optional.of(this.getClass().getName()), Optional.of("close"));
        this.outAvailable.set(false);
        this.inAvailable.set(false);
    }

    /**
     * Method to launch the input stream of the handler in a separate thread.
     */

    /**
     * Method invoked when sending a message to the output stream. This method is thread safe.
     * @param m message to be sent
     * @return true if the message has been sent correctly
     */
    public boolean sendMessage(Message m){
        synchronized (this.outLock) {
            LoggerManager.getInstance().mutableInfo("Lock acquired...", Optional.of(this.getClass().getName()), Optional.of("sendMessage"));
            // Here I am double locking but there is no deadlock since the input thread will never lock on the outLock
            if (!this.outAvailable.get()) {
                LoggerManager.getInstance().mutableInfo("Not yet ready to send message! Try again...", Optional.of(this.getClass().getName()), Optional.of("sendMessage"));
                return false;
            }

            try {
                LoggerManager.getInstance().mutableInfo("Sending message: " + m.getClass().getName() + " to: "+remoteNodeName.getIP() +": " + remoteNodeName.getPort(), Optional.of(this.getClass().getName()), Optional.of("sendMessage"));
                // Important. synchronize everything in the output stream
                this.out.writeObject(m);
                this.out.flush();
            } catch (IOException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                // TODO: what to do ?
                return false;
            }

            return true;
        }
    }

    /**
     * Get the name of the remote socket
     * @return the name of the remote socket
     */
    public NodeName getRemoteNodeName(){
        return this.remoteNodeName;
    }

    protected void startPingPong(boolean isFirstPing){
        pingPongManager = new PingPongManager(manager,this, isFirstPing);
    }

    protected void stopPingPong(){
        pingPongManager.stopPingPong();
    }


}
