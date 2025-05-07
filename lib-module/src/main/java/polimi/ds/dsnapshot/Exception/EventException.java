package polimi.ds.dsnapshot.Exception;

/**
 * Exception stating that there a generic event exception has happened.
 */
public class EventException extends Exception {
    /**
     * Constructor of the exception.
     * @param message Message containing the information relative to the error.
     */
    public EventException(String message) {
        super(message);
    }
}
