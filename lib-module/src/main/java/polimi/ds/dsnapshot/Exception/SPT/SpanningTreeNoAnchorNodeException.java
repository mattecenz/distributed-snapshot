package polimi.ds.dsnapshot.Exception.SPT;

/**
 * Exception stating that the current node does not have an anchor node.
 * This could be due to the fact that the first node of the network does not have one, or due to a crash.
 */
public class SpanningTreeNoAnchorNodeException extends Exception {
    /**
     * Constructor of the exception.
     */
    public SpanningTreeNoAnchorNodeException() {
        super("The SPT does not contain the anchor node!");
    }
}
