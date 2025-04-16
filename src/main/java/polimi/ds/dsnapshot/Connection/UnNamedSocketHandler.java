package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.Exit.AdoptionRequestMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.DirectConnectionMsg;
import polimi.ds.dsnapshot.Connection.Messages.Join.JoinMsg;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Exception.RoutingTableNodeAlreadyPresentException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Class for any socket connected which has not yet a name in the network.
 * In order to be promoted in the network a JoinMessage needs to be sent.
 */
public class UnNamedSocketHandler implements Runnable{

    /**
     * Socket of the connection
     */
    private final Socket socket;

    /**
     * Input stream of the socket.
     * This class has no output as it its not writable
     */
    private ObjectInputStream in;

    /**
     * Connection manager to be notified when a message arrived
     */
    private final ConnectionManager connectionManager;

    /**
     * Constructor of the unnamed socket handler
     * @param socket socket received
     * @param connectionManager manager called when a message is received
     */
    public UnNamedSocketHandler(Socket socket, ConnectionManager connectionManager) {
        this.socket = socket;
        this.connectionManager = connectionManager;
    }

    /**
     * Getter of the socket
     * @return the socket of the remote unnamed node
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * Getter of the input stream
     * @return the object input stream related to this socket
     */
    public ObjectInputStream getIn() {
        return this.in;
    }

    /**
     * Starts the thread which waits for incoming messages.
     * If the message received is a JoinMessage then it activates the procedure in the ConnectionManager
     * and passes the reference to a ClientSocketHandler afterwards.
     */
    @Override
    public void run() {

        try{
            LoggerManager.getInstance().mutableInfo("Creating input stream...", Optional.of(this.getClass().getName()), Optional.of("run"));
            this.in=new ObjectInputStream(this.socket.getInputStream());
        }catch (IOException e){
            // TODO: what to do ?
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
        }

        // Read a generic message and decide what to do
        boolean finished=false;
        while(!finished){
            try {
                LoggerManager.getInstance().mutableInfo("Listening..", Optional.of(this.getClass().getName()), Optional.of("run"));
                Message m = (Message) this.in.readObject();
                LoggerManager.getInstance().mutableInfo("Message received: " +m.getClass().getName(), Optional.of(this.getClass().getName()), Optional.of("run"));

                switch (m.getInternalID()){
                    case MESSAGE_JOIN -> {
                        this.connectionManager.receiveNewJoinMessage( (JoinMsg) m, this);
                        finished=true;
                    }
                    case MESSAGE_DIRECTCONNECTION -> {
                        this.connectionManager.receiveNewDirectConnectionMessage((DirectConnectionMsg) m, this);
                        finished=true;
                    }
                    case MESSAGE_ADOPTION_REQUEST ->{
                        this.connectionManager.receiveAdoptionOrJoinRequest((AdoptionRequestMsg) m, this);
                        finished=true;
                    }
                    case null, default -> {
                        LoggerManager.instanceGetLogger().log(Level.WARNING, "Received a message which is not a join, do not do nothing. ");
                    }

                }
            } catch (IOException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "IO exception", e);
                // TODO: what to do ?
            }catch (ClassNotFoundException e){
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "ClassNotFoundException", e);
                // TODO: what to do ?
            } catch (RoutingTableNodeAlreadyPresentException e) {
                LoggerManager.instanceGetLogger().log(Level.SEVERE, "RoutingTableNodeAlreadyPresentException", e);
                // TODO: what to do ?
            }
        }

    }
}
