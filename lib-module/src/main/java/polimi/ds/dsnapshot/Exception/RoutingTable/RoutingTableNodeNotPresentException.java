package polimi.ds.dsnapshot.Exception.RoutingTable;

/**
 * Exception stating that we are asking for a node not currently present in the routing table.
 */
public class RoutingTableNodeNotPresentException extends Exception {
    /**
     * Constructor of the exception.
     */
    public RoutingTableNodeNotPresentException() {
        super("Node not present in the routing table!");
    }
}
