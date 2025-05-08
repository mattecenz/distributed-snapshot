package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Generic exception exported by the library to the application.
 */
public class DSException extends Exception {
    /**
     * Constructor of the exception.
     * @param message String message related to the specific exception thrown.
     */
    public DSException(String message) {
        super(message);
    }
}
