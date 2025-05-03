package polimi.ds.dsnapshot.Exception.SPT;

public class SpanningTreeNoAnchorNodeException extends Exception {
    public SpanningTreeNoAnchorNodeException() {
        super("The SPT does not contain the anchor node!");
    }
}
