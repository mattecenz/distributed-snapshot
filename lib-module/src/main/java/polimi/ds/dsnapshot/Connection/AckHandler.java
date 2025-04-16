package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Exception.AckHandlerAlreadyRemovedException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.util.*;
import java.util.logging.Level;

public class AckHandler{
    /**
     * Data structure for pending messages which need acknowledgment and the relative
     * handle to be invoked when needed
     */
    private final Map<Integer,Object> acksPending;

    /**
     * Constructor of the ack handler.
     */
    public AckHandler() {
        this.acksPending = new HashMap<>();
    }

    /**
     * Insert in the data structure the id of the message that waits for his ack.
     * It is an atomic operation
     * @param ack unique id found in the Message class
     * @param lock lock where the thread is waiting
     */
    public synchronized void insertAckId(int ack, Object lock) {
        LoggerManager.getInstance().mutableInfo("inserting ack: " + ack + ".", Optional.of(this.getClass().getName()), Optional.of("insertAckId"));
        this.acksPending.put(ack,lock);
    }

    /**
     * Remove specified id And notify the waiting thread.
     * It is an atomic operation
     * @param ack id of the message to remove
     */
    public synchronized void removeAckId(int ack) throws AckHandlerAlreadyRemovedException {

        Object toNotify = this.acksPending.remove(ack);
        // this should never happen
        if(toNotify == null){
            LoggerManager.getInstance().mutableInfo("No thread found waiting for ack " + ack + ".", Optional.of(this.getClass().getName()), Optional.of("removeAckId"));
            throw new AckHandlerAlreadyRemovedException();
        }
        // Notify the object by notifying the thread waiting on that lock.
        synchronized (toNotify){
            LoggerManager.getInstance().mutableInfo("removing ack: " + ack + ".", Optional.of(this.getClass().getName()), Optional.of("removeAckId"));
            toNotify.notifyAll();
        }
    }
}
