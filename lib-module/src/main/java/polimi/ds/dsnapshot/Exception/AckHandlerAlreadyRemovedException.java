package polimi.ds.dsnapshot.Exception;

/**
 * Exception stating that the ack has been already removed from the internal handler.
 */
public class AckHandlerAlreadyRemovedException extends Exception {
    /**
     * Constructor of the exception.
     */
    public AckHandlerAlreadyRemovedException() {super("Ack already removed from the list.");}
}
