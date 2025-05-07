package polimi.ds.dsnapshot.Connection.Messages;

/**
 * Wrapper class to guarantee mutual exclusive access to the next unique sequence number of the message.
 * This sequence number can be potentially used to enforce an ordering of messages, but here none is provided.
 * Its only purpose is in the exchange of acks.
 */
public class MessageSQN {

    /**
     * Lock to ensure mutual exclusion.
     */
    private static final Object lock = new Object();

    /**
     * Unique sequence number generated for each message.
     */
    private static Integer seq = 0;

    /**
     * Getter of the unique id. The method is thread safe.
     * @return Unique integer id.
     */
    public static int getNextSequenceNumber(){
        synchronized (lock){
            // If i am not mistaken when returning the number the synchronized is dropped correctly
            // Also this should return a copy of the object
            return ++seq;
        }
    }

}
