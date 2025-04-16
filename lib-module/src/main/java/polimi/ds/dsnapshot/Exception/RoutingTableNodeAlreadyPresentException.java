package polimi.ds.dsnapshot.Exception;

public class RoutingTableNodeAlreadyPresentException extends Exception {
    public RoutingTableNodeAlreadyPresentException() {
        super("Node is already present in the routing table!");
    }
}
