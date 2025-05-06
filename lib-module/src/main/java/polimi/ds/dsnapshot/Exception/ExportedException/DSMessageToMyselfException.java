package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Exception stating that someone is trying to send a message to himself.
 * It is an extension of the generic exported DSException.
 */
public class DSMessageToMyselfException extends DSException {
    /**
     * Constructor of the exception.
     */
    public DSMessageToMyselfException() {
        super("You are trying to send a message to yourself! ");
    }
}
