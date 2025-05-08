package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Exception stating that the port used by the TCP connection is already in use,
 * hence it is not possible to open a new connection in it.
 * It is an extension of the generic exported DSException.
 */
public class DSPortAlreadyInUseException extends DSException {
    /**
     * Constructor of the exception.
     */
    public DSPortAlreadyInUseException() { super("The port is already in use!"); }
}
