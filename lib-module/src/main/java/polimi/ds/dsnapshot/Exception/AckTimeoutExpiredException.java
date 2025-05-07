package polimi.ds.dsnapshot.Exception;

/**
 * Exception stating that the timeout for the ack arrival has expired, hence probably the packet was lost.
 */
public class AckTimeoutExpiredException extends Exception {
    /**
     * Constructor of the exception.
     */
    public AckTimeoutExpiredException() {
        super("The ack timeout expired. Need to take action!");
    }
}
