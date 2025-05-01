package polimi.ds.dsnapshot.Exception;

public class AckTimeoutExpiredException extends Exception {
    public AckTimeoutExpiredException() {
        super("The ack timeout expired. Need to take action!");
    }
}
