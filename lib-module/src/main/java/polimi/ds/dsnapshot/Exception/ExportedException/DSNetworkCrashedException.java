package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Exception stating that since a node has crashed, the network is halted and the operation may have been lost.
 * It is an extension of the generic exported DSException.
 */
public class DSNetworkCrashedException extends DSException {
    /**
     * Constructor of the exception.
     */
    public DSNetworkCrashedException() {super("Some client in the network crashed. It is not possible to send messages anymore.");}
}
