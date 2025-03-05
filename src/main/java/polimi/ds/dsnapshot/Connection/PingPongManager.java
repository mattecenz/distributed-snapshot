package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.PingPongMessage;

import java.util.function.Consumer;

public class PingPongManager {
    private final Object pingLock = new Object();
    private final Object pongLock = new Object();
    private final int pingPongTimeout = 5000; //todo: config param
    Thread pingThread;
    Thread pongThread;
    private Boolean pingResponse = false;
    private boolean pongResponse = false;

    Consumer<Message> sendMessage;
    boolean mute = false;

    protected PingPongManager(Consumer<Message> sendMessage, boolean mute) {
        this.mute = mute;
        this.sendMessage = sendMessage;
        sendMessage.accept(new PingPongMessage(true, true));
        pingThread = new Thread(this::ping);
        pongThread = new Thread(this::waitForPing);
        pingThread.start();
    }


    private void ping(){
            try {
                while(true) {
                    Thread.sleep(pingPongTimeout);
                    sendMessage.accept(new PingPongMessage(false, true));
                    pingThread.wait(pingPongTimeout);
                    if (!mute) System.out.println("[SocketHandler] Ping sent!");
                    synchronized (pingLock) {
                        if (!pingResponse) {
                            this.pingFail();
                            return;
                        }
                        pingResponse = false;
                    }
                }
            } catch (InterruptedException e) {
                //todo: manage exception
                System.out.println("[SocketHandler] error while waiting for pong " + e.getMessage());
            }

    }

    protected void pong(){
        if(!mute) System.out.println("[SocketHandler] Pong sent!");
        sendMessage.accept(new PingPongMessage(false, false));
        if(pongThread.isAlive()) pongThread.interrupt();
        synchronized (pongLock) {
            pongResponse = false;
        }
        pongThread.start();
    }
    protected void pongResponse(){
        if(!mute) System.out.println("[SocketHandler] Pong received!");
        synchronized (pingLock) {
            pingResponse = true;
        }
        pingThread.notify();
    }

    protected void pingResponse(){
        synchronized (pongLock) {
            pongResponse = true;
            pongThread.notify();
        }
    }

    private void waitForPing(){
        try {
            pongThread.wait(2*pingPongTimeout);
            synchronized (pongLock) {
                if(!pongResponse){
                    this.pongFail();
                }
            }
        } catch (InterruptedException e) {
            //todo: manage exception
            System.out.println("[SocketHandler] error while waiting for pong response" + e.getMessage());
        }
    }

    private void pingFail(){
        System.out.println("[SocketHandler] Ping failed!");
        //todo: React to ping pong fail
    }

    private void pongFail(){
        System.out.println("[SocketHandler] Pong failed!");
        //todo
    }
}
