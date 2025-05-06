package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Exception stating that the application tried to reconnect to a parent which did not crash.
 * It is an extension of the generic exported DSException.
 */
public class DSParentNotCrashedException extends DSException {
    /**
     * Constructor of the exception.
     */
    public DSParentNotCrashedException() {super("The parent has not crashed, no need to reconnect.");}
}
