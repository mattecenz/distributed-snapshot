package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.PingPongMessage;

import java.util.function.Consumer;

public class PingPongManager {
    private final Object pingLock = new Object();
    private final int pingPongTimeout = 5000; //todo: config param
    Thread pingThread;
    //Thread pongThread;
    private Boolean pingPongResponse = false;

    Consumer<Message> sendMessage;
    boolean mute = false;

    protected PingPongManager(Consumer<Message> sendMessage, boolean mute) {
        this.mute = mute;
        this.sendMessage = sendMessage;
        sendMessage.accept(new PingPongMessage(true));
        pingThread = new Thread(this::ping);
    }


    private void ping(){
            try {
                Thread.sleep(pingPongTimeout);
                sendMessage.accept(new PingPongMessage(false));
                pingThread.wait(pingPongTimeout);
                if(!mute) System.out.println("[SocketHandler] Ping sent!");
                synchronized (pingLock) {
                    if (!pingPongResponse) {
                        this.pingFail();
                        return;
                    }
                    pingPongResponse = false;
                }
                this.ping();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

    }

    protected void pong(){
        if(!mute) System.out.println("[SocketHandler] Pong sent!");
        this.PongResponse();
        sendMessage.accept(new PingPongMessage(false));
        //todo: detect if ping is missing from to much time (or bilateral ping pong)
    }
    protected void PongResponse(){
        if(!mute) System.out.println("[SocketHandler] Pong received!");
        synchronized (pingLock) {
            pingPongResponse = true;
        }
        pingThread.notify();
    }
    //todo: first ping add child

    private void pingFail(){
        //todo: React to ping pong fail
    }
}
