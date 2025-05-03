package polimi.ds.dsnapshot.Exception.RoutingTable;

public class RoutingTableNodeNotPresentException extends Exception {
    public RoutingTableNodeNotPresentException() {
        super("Node not present in the routing table!");
    }
}
