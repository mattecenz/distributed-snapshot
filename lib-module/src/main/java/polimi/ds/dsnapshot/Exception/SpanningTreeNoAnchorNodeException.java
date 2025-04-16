package polimi.ds.dsnapshot.Exception;

public class SpanningTreeNoAnchorNodeException extends Exception {
    public SpanningTreeNoAnchorNodeException() {
        super("The SPT does not contain the anchor node!");
    }
}
