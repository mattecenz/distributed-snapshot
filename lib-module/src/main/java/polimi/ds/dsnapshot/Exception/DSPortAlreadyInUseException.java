package polimi.ds.dsnapshot.Exception;

public class DSPortAlreadyInUseException extends Exception {
    public DSPortAlreadyInUseException() {
        super("The port is already in use!");
    }
}
