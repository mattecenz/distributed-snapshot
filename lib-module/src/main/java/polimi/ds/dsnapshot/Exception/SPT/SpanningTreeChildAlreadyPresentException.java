package polimi.ds.dsnapshot.Exception.SPT;

/**
 * TODO: this exception should just extend Exception not the runtime.
 * Exception stating that you are trying to add an already existing child to the spanning tree.
 */
public class SpanningTreeChildAlreadyPresentException extends RuntimeException {
    /**
     * Constructor of the exception.
     */
    public SpanningTreeChildAlreadyPresentException() {
        super("The spanning tree already contains this child you want to add!");
    }
}
