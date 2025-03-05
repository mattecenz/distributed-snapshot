package polimi.ds.dsnapshot.Connection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AckHandler{
    /**
     * Data structure for pending messages which need acknowledgment and the relative
     * handle to be invoked when needed
     */
    private final Map<Integer,Thread> acksPending;

    /**
     * Constructor of the ack handler.
     */
    public AckHandler() {
        this.acksPending = new HashMap<>();
    }

    /**
     * Insert in the data structure the id of the message that waits for his ack
     * @param ack unique id found in the Message class
     * @param handle handle of the thread to be notified
     */
    public synchronized void insertAckId(int ack, Thread handle) {
        this.acksPending.put(ack, handle);
    }

    /**
     * Remove specified id And notify the waiting thread
     * @param ack id of the message to remove
     */
    public synchronized void removeAckId(int ack){
        Thread toNotify = this.acksPending.remove(ack);
        // this should never happen
        if(toNotify == null){
            throw new RuntimeException("[ConnectionManager] No thread found in the ack map for ack " + ack);
        }
        toNotify.interrupt();
    }
}
