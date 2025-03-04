package polimi.ds.dsnapshot.Connection;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AckHandler{
    /**
     * Data structure for pending messages which need acknowledgment
     */
    private final HashSet<Integer> acksPending;
    /**
     * Read Write lock used to access to the internal data structure
     */
    private final ReentrantReadWriteLock lock;
    /**
     * Read Lock
     */
    private final Lock readLock;
    /**
     * Write Lock
     */
    private final Lock writeLock;

    /**
     * Constructor of the ack handler.
     * It uses a fair reentrant read write lock
     */
    public AckHandler() {
        this.acksPending = new HashSet<>();
        this.lock = new ReentrantReadWriteLock(true);
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Insert in the data structure the id of the message that waits for his ack
     * @param ack unique id found in the Message class
     */
    public void insertAckId(int ack) {
        this.writeLock.lock();
        this.acksPending.add(ack);
        this.writeLock.unlock();
    }

    /**
     * Method to check if the id of the message is still in the data structure
     * @param ack id of the message to check
     * @return true if the id is still in the data structure
     */
    public boolean isAckIdPresent(int ack) {
        this.readLock.lock();
        boolean res = this.acksPending.contains(ack);
        this.readLock.unlock();
        return res;
    }

    /**
     * Remove specified id.
     * @param ack id of the message to remove
     */
    public void removeAckId(int ack){
        this.writeLock.lock();
        this.acksPending.remove(ack);
        this.writeLock.unlock();
    }
}
