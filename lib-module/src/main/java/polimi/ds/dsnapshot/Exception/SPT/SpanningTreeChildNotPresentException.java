package polimi.ds.dsnapshot.Exception.SPT;

/**
 * Exception stating that the child you are trying to access is not present in the current spanning tree.
 */
public class SpanningTreeChildNotPresentException extends RuntimeException {
    /**
     * Constructor of the exception.
     */
    public SpanningTreeChildNotPresentException() {
        super("The child socket handler is not present in the SPT!" );
    }
}
