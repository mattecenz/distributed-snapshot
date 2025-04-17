package polimi.ds.dsnapshot.Exception;

public class DSNodeUnreachableException extends Exception {
    public DSNodeUnreachableException() {
        super("The node you are trying to reach is unreachable ! ");
    }
}
