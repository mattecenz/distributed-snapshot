package polimi.ds.dsnapshot.Exception.ExportedException;

/**
 * Exception stating that the node you are trying to contact (maybe join or send messages to) is unreachable,
 * probably because it is not present in the network.
 * It is an extension of the generic exported DSException.
 */
public class DSNodeUnreachableException extends DSException {
    /**
     * Constructor of the exception.
     */
    public DSNodeUnreachableException() {
        super("The node you are trying to reach is unreachable ! ");
    }
}
