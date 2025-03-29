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
        try {
            manager.sendMessageSynchronized(new PingPongMessage(isFirstPing),handler);
        } catch (ConnectionException e) {
            pingFail();
            return;
        }
        //startThread
        ThreadPool.submit(this::sendPing);
    }

    private void sendPing(){
        try {
            while (true) {
                Thread.sleep(pingPongTimeout);
                manager.sendMessageSynchronized(new PingPongMessage(true), handler);
            }
        } catch (InterruptedException e) {
            //todo: manage exception
            LoggerManager.instanceGetLogger().log(Level.SEVERE, "[PingPongManager] error while waiting for pong ", e);
        } catch (ConnectionException e){
            pingFail();
            return;
        }
    }

    private void pingFail(){
        LoggerManager.instanceGetLogger().warning("unanswered ping message with" + handler.getRemoteNodeName().getIP() + ":" + handler.getRemoteNodeName().getPort());
        //todo: React to ping pong fail
    }

}
