package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.PingPongMessage;
import polimi.ds.dsnapshot.Exception.ConnectionException;
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.ThreadPool;


public class PingPongManager {
    private boolean mute = false;
    private final ClientSocketHandler handler;
    private final ConnectionManager manager;
    private final int pingPongTimeout = Config.PINGPONG_TIMEOUT;


    protected PingPongManager(ConnectionManager connectionManager ,ClientSocketHandler handler, boolean mute) {
        this.mute = mute;
        manager = connectionManager;
        this.handler = handler;
        //send first ping
        try {
            manager.sendMessageSynchronized(new PingPongMessage(true),handler);
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
                if(!this.mute) System.out.println("[PingPongManager] send ping");
                Thread.sleep(pingPongTimeout);
                manager.sendMessageSynchronized(new PingPongMessage(true), handler);
            }
        } catch (InterruptedException e) {
            //todo: manage exception
            System.out.println("[PingPongManager] error while waiting for pong " + e.getMessage());
        } catch (ConnectionException e){
            pingFail();
            return;
        }
    }

    private void pingFail(){
        System.out.println("[PingPongManager] Ping failed!");
        //todo: React to ping pong fail
    }

}
