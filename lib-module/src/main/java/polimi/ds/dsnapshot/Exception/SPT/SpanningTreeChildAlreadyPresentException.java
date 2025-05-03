package polimi.ds.dsnapshot.Exception.SPT;

public class SpanningTreeChildAlreadyPresentException extends RuntimeException {
    public SpanningTreeChildAlreadyPresentException() {
        super("The spanning tree already contains this child you want to add!");
    }
}
