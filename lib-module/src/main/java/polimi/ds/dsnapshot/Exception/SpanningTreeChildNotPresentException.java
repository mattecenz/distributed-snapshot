package polimi.ds.dsnapshot.Exception;

public class SpanningTreeChildNotPresentException extends RuntimeException {
    public SpanningTreeChildNotPresentException() {
        super("The child socket handler is not present in the SPT!" );
    }
}
