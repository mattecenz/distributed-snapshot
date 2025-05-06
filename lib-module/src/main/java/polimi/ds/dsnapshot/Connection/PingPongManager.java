package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.PingPongMessage;
import polimi.ds.dsnapshot.Exception.AckTimeoutExpiredException;
import polimi.ds.dsnapshot.Exception.SocketClosedException;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Manager of the ping pong messages between two nodes connected by an edge in the spanning tree.
 */
public class PingPongManager {
    /**
     * Handler of the socket which exchanges the ping pong.
     */
    private final ClientSocketHandler handler;
    /**
     * Reference to the connection manager.
     */
    private final ConnectionManager manager;
    /**
     * Constant which determines how much the ping pong procedure should wait before it can assume the network crashed.
     */
    private final int pingPongTimeout = Config.getInt("network.PingPongTimeout");
    /**
     * Future object used for thread variables.
     */
    Future<?> pingPongFuture;

    /**
     * Constructor of the ping pong manager.
     * @param connectionManager Reference to the connection manager.
     * @param handler Reference to the socket handler with the input and output stream.
     * @param isFirstPing Boolean indicating if the message received is the first ping.
     */
    protected PingPongManager(ConnectionManager connectionManager ,ClientSocketHandler handler, boolean isFirstPing) {
        LoggerManager.getInstance().mutableInfo("start ping pong with: " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("PingPongManager"));
        manager = connectionManager;
        this.handler = handler;
        //send first ping
        pingPongFuture = ThreadPool.submitAndReturnFuture(() -> {sendFirstPing(isFirstPing);});
    }

    /**
     * Method to send the first ping through the socket.
     * If the timeout expires, it initiates the failure procedure.
     * @param isFirstPing Boolean indicating if the message is the first ping.
     */
    private void sendFirstPing(boolean isFirstPing){
        LoggerManager.getInstance().mutableInfo("send first ping", Optional.of(this.getClass().getName()), Optional.of("sendFirstPing"));
        PingPongMessage pingPongMessage = null;
        try {
            pingPongMessage = new PingPongMessage(isFirstPing);
            manager.sendMessageSynchronized(pingPongMessage,handler);
        } catch (AckTimeoutExpiredException e) {
            pingFail(pingPongMessage.getSequenceNumber());
            return;
        } catch (SocketClosedException e) {
            LoggerManager.getInstance().mutableInfo("The socket has been closed. It is not possible to send messages anymore.", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            return;
        }
        //startThread
        LoggerManager.getInstance().mutableInfo("first ping successfully sent", Optional.of(this.getClass().getName()), Optional.of("sendFirstPing"));
        this.sendPing();
    }

    /**
     * Method which sends a periodic ping through the socket connection.
     * It stops when the connection is down, and if the ping is not responded with a pong a failure procedure is initiated.
     */
    private void sendPing(){
        LoggerManager.getInstance().mutableInfo("start periodic ping", Optional.of(this.getClass().getName()), Optional.of("sendPing"));
        PingPongMessage pingPongMessage = null;
        try {
            while (!Thread.currentThread().isInterrupted() && !pingPongFuture.isCancelled()) {
                pingPongMessage = new PingPongMessage(false);
                manager.sendMessageSynchronized(pingPongMessage, handler);
                Thread.sleep(pingPongTimeout);
            }
        } catch (InterruptedException e) {
            //todo: manage exception
            LoggerManager.getInstance().mutableInfo( "[PingPongManager] ping pong with node: " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort()+ " has been stopped throwing InterruptedException", Optional.of(this.getClass().getName()), Optional.of("sendPing"));
            return;
        } catch (AckTimeoutExpiredException e){
            pingFail(pingPongMessage.getSequenceNumber());
            manager.initiateCrashProcedure(handler);
            return;
        } catch (SocketClosedException e) {
            LoggerManager.getInstance().mutableInfo("The socket has been closed. It is not possible to send messages anymore.", Optional.of(this.getClass().getName()), Optional.of("sendMessageSynchronized"));
            manager.initiateCrashProcedure(handler);
            return;
        }
        LoggerManager.getInstance().mutableInfo( "[PingPongManager] ping pong with node: " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort()+ " has been stopped without exception", Optional.of(this.getClass().getName()), Optional.of("sendPing"));
    }

    /**
     * Method to manually stop the ping pong execution.
     */
    synchronized public void stopPingPong(){
        boolean c = pingPongFuture.cancel(true);
        LoggerManager.getInstance().mutableInfo("[PingPongManager] stop ping pong "+c , Optional.of(this.getClass().getName()), Optional.of("stopPingPong"));
    }

    /**
     * Method to initiate the failure procedure when a pong is not received.
     * @param sequenceNumber Sequence number of the message who crashed.
     */
    private void pingFail(int sequenceNumber){
        LoggerManager.instanceGetLogger().warning("unanswered ping message with " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort() + " [sequence number: " + sequenceNumber + " ]");
        //todo: React to ping pong fail
    }

}
