package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.PingPongMessage;
import polimi.ds.dsnapshot.Exception.AckTimeoutExpiredException;
import polimi.ds.dsnapshot.Exception.SocketClosedException;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.util.Optional;
import java.util.concurrent.Future;


public class PingPongManager {
    private final ClientSocketHandler handler;
    private final ConnectionManager manager;
    private final int pingPongTimeout = Config.getInt("network.PingPongTimeout");
    Future<?> pingPongFuture;

    protected PingPongManager(ConnectionManager connectionManager ,ClientSocketHandler handler, boolean isFirstPing) {
        LoggerManager.getInstance().mutableInfo("start ping pong with: " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("PingPongManager"));
        manager = connectionManager;
        this.handler = handler;
        //send first ping
        pingPongFuture = ThreadPool.submitAndReturnFuture(() -> {sendFirstPing(isFirstPing);});
    }

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

    synchronized public void stopPingPong(){
        boolean c = pingPongFuture.cancel(true);
        LoggerManager.getInstance().mutableInfo("[PingPongManager] stop ping pong "+c , Optional.of(this.getClass().getName()), Optional.of("stopPingPong"));
    }

    private void pingFail(int sequenceNumber){
        LoggerManager.instanceGetLogger().warning("unanswered ping message with " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort() + " [sequence number: " + sequenceNumber + " ]");
        //todo: React to ping pong fail
    }

}
