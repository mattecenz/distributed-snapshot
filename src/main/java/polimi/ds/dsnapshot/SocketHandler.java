package polimi.ds.dsnapshot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

class SocketHandler implements Runnable{

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
     * Mute attribute of the server
     */
    private boolean mute = false;

    /**
     * Boolean to check  if the socket handler is ready
     */
    private final AtomicBoolean available;

    /**
     * Shared variable used for checking if the server is still listening or not
     */
    private final AtomicBoolean listening;

    /**
     * Constructor of the handler
     * @param socket socket to be managed
     */
    public SocketHandler(Socket socket) {
        this.socket = socket;
        this.available = new AtomicBoolean(false);
        this.listening = new AtomicBoolean(false);

        System.out.println("[SocketHandler] Socket connected at address: " + socket.getInetAddress() + ":" + socket.getPort());
    }

    /**
     * Constructor of the handler with the mute option
     * @param socket socket to be managed
     * @param mute specify if the handler is mute or not
     */
    public SocketHandler(Socket socket, boolean mute) {
        this(socket);
        this.mute = mute;
    }

    /**
     * Method launched when the thread is submitted
     */
    @Override
    public void run() {
        // Set the timeout
        try {
            if(!this.mute) System.out.print("[SocketHandler] Setting socket timeout... ");
            // From the doc it says that when reading in this socket this is the max time (in ms) which the thread
            // will sleep, else an exception is generated.
            // TODO: wrap in a utils class
            this.socket.setSoTimeout(5000);
            if(!this.mute) System.out.println("done");
        }
        catch (SocketException e) {
            //TODO: what to do ?
            System.err.println("Socket exception: " + e.getMessage());
        }

        // Create the object output stream
        try{
            if(!this.mute) System.out.print("[SocketHandler] Creating the output stream... ");
            this.out=new ObjectOutputStream(this.socket.getOutputStream());
            if(!this.mute) System.out.println("done");
        }
        catch (IOException e){
            //TODO: what to do ?
            System.err.println("IO exception: " + e.getMessage());
        }

        // Now we need two threads. This one we will use it as sender of messages, the other will be used as receiver.

        this.launchInboundMessagesThread();

    }

    private void launchInboundMessagesThread(){

        //NB: we notice that this.mute is a shared variable, but always accessed as a read, so no problem there.

        if(!this.mute) System.out.print("[SocketHandler] Creating input stream thread... ");

        Thread t = new Thread( ()->{
                // Create the input stream as above
            try{
                if(!this.mute) System.out.print("[SocketHandlerIN] Creating input stream...");
                this.in=new ObjectInputStream(this.socket.getInputStream());
                if(!this.mute) System.out.println("done");
            }catch (IOException e){
                // TODO: what to do ?
                System.err.println("IO exception: " + e.getMessage());
            }

            // Now we are ready to listen
            this.listening.set(true);

            // Read a generic message and decide what to do
            while(listening.get()){
                try {
                    if(!this.mute) System.out.println("[SocketHandlerIN] Listening... ");
                    Message m = (Message) this.in.readObject();
                    if(!this.mute) System.out.println("[SocketHandlerIN] Message received!");
                    // TODO: handle the message
                } catch (IOException e) {
                    System.err.println("[SocketHandlerIN] IO exception: " + e.getMessage());
                    // TODO: what to do ?
                }catch (ClassNotFoundException e){
                    System.err.println("[SocketHandlerIN] ClassNotFoundException: " + e.getMessage());
                    // TODO: what to do ?
                }
            }

            }
        );

        if(!mute) System.out.println("done");
        if(!mute) System.out.print("[SocketHandler] Launching input stream thread... ");

        t.start();

        if(!mute) System.out.println("done");

        this.available.set(true);

        // This function is done
    }
}
