package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Events.Event;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
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
     */
    public ClientSocketHandler(Socket socket, NodeName remoteNodeName, ConnectionManager manager) {
        this.socket = socket;
        this.remoteNodeName = remoteNodeName;
        this.outAvailable = new AtomicBoolean(false);
        this.inAvailable = new AtomicBoolean(false);
        this.manager = manager;
        this.outLock = new Object();

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

        this.outLock = new Object();

        // This is the fundamental difference
        this.in = unhandler.getIn();

        this.prepareMessageInputEvent(JavaDistributedSnapshot.getInstance());

        LoggerManager.getInstance().mutableInfo("Socket connected at address:" + socket.getInetAddress() + ":" + socket.getPort(), Optional.of(this.getClass().getName()), Optional.of("ClientSocketHandler"));
    }

    /**
     * Constructor of the handler without the connection manager.
     * USE ONLY FOR TESTING !!!!!!!!!!!!!!!!!!!!!!
     * @param socket socket to be managed
     * @param remoteNodeName name of the remote node
     */
    public ClientSocketHandler(Socket socket, NodeName remoteNodeName) {
        this(socket, remoteNodeName, null);
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
        LoggerManager.getInstance().mutableInfo("Setting socket timeout...", Optional.of(this.getClass().getName()), Optional.of("run"));
        // From the doc it says that when reading in this socket this is the max time (in ms) which the thread
        // will sleep, else an exception is generated.
        //this.socket.setSoTimeout(Config.getInt("network.PingPongTimeout")*2);

        // Create the object output stream
        try{
            LoggerManager.getInstance().mutableInfo("Creating the output stream...", Optional.of(this.getClass().getName()), Optional.of("run"));
            this.out=new ObjectOutputStream(this.socket.getOutputStream());

            this.outAvailable.set(true);
        }
        catch (IOException e){
            //TODO: what to do ?
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
        }

        // Now we need two threads. This one we will use it as sender of messages, the other will be used as receiver.

        this.launchInboundMessagesThread();
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
    private void launchInboundMessagesThread(){

        //NB: we notice that Config.SNAPSHOT_MUTE is a shared variable, but always accessed as a read, so no problem there.
        LoggerManager.getInstance().mutableInfo("Creating input stream thread...", Optional.of(this.getClass().getName()), Optional.of("launchInboundMessagesThread"));

        Thread t = new Thread( ()->{
                // Create the input stream as above
            if(!this.inAvailable.get()) {
                try {
                    LoggerManager.getInstance().mutableInfo("Creating input stream..", Optional.of(this.getClass().getName()), Optional.of("launchInboundMessagesThread"));
                    this.in = new ObjectInputStream(this.socket.getInputStream());
                } catch (IOException e) {
                    // TODO: what to do ?
                    LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                }
                // Now we are ready to listen incoming messages
                this.inAvailable.set(true);
            }

            // Read a generic message and decide what to do
            while(inAvailable.get()){
                try {
                    LoggerManager.getInstance().mutableInfo("Listening..", Optional.of(this.getClass().getName()+this.hashCode()), Optional.of("launchInboundMessagesThread"));
                    Message m = (Message) this.in.readObject();
                    LoggerManager.getInstance().mutableInfo("Message received: " +m.getClass().getName(), Optional.of(this.getClass().getName()+this.hashCode()), Optional.of("launchInboundMessagesThread"));
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
        );

        LoggerManager.getInstance().mutableInfo("Launching input stream thread...", Optional.of(this.getClass().getName()), Optional.of("launchInboundMessagesThread"));

        t.start();

        // This function is done
    }

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
                LoggerManager.getInstance().mutableInfo("Sending message: " + m.getClass().getName(), Optional.of(this.getClass().getName()), Optional.of("sendMessage"));
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


}
