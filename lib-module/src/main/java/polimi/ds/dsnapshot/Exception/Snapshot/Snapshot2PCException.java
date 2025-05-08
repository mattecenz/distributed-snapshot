package polimi.ds.dsnapshot.Exception.Snapshot;

/**
 * Exception stating that some error has happened during the 2PC phase.
 */
public class Snapshot2PCException extends Exception {
    /**
     * Constructor of the exception.
     * @param message Message containing the information of the error.
     */
    public Snapshot2PCException(String message) {
        super(message);
    }
}
