package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.PingPongMessage;
import polimi.ds.dsnapshot.Exception.ConnectionException;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.ThreadPool;

import java.util.Optional;
import java.util.logging.Level;


public class PingPongManager {
    private final ClientSocketHandler handler;
    private final ConnectionManager manager;
    private final int pingPongTimeout = Config.getInt("network.PingPongTimeout");


    protected PingPongManager(ConnectionManager connectionManager ,ClientSocketHandler handler, boolean isFirstPing) {
        LoggerManager.getInstance().mutableInfo("start ping pong with: " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("PingPongManager"));
        manager = connectionManager;
        this.handler = handler;
        //send first ping
        ThreadPool.submit(() -> {sendFirstPing(isFirstPing);});
    }

    private void sendFirstPing(boolean isFirstPing){
        LoggerManager.getInstance().mutableInfo("send first ping", Optional.of(this.getClass().getName()), Optional.of("sendFirstPing"));
        PingPongMessage pingPongMessage = null;
        try {
            pingPongMessage = new PingPongMessage(isFirstPing);
            manager.sendMessageSynchronized(pingPongMessage,handler);
        } catch (ConnectionException e) {
            pingFail(pingPongMessage.getSequenceNumber());
            return;
        }
        //startThread
        LoggerManager.getInstance().mutableInfo("first ping successfully sent", Optional.of(this.getClass().getName()), Optional.of("sendFirstPing"));
        ThreadPool.submit(this::sendPing);
    }

    private void sendPing(){
        LoggerManager.getInstance().mutableInfo("start periodic ping", Optional.of(this.getClass().getName()), Optional.of("sendPing"));
        PingPongMessage pingPongMessage = null;
        try {
            while (true) {
                pingPongMessage = new PingPongMessage(false);
                manager.sendMessageSynchronized(pingPongMessage, handler);
                Thread.sleep(pingPongTimeout);
            }
        } catch (InterruptedException e) {
            //todo: manage exception
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "[PingPongManager] error while waiting for pong ", e);
            return;
        } catch (ConnectionException e){
            pingFail(pingPongMessage.getSequenceNumber());
            return;
        }
    }

    private void pingFail(int sequenceNumber){
        LoggerManager.instanceGetLogger().warning("unanswered ping message with " + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort() + " [sequence number: " + sequenceNumber + " ]");
        //todo: React to ping pong fail
    }

}
