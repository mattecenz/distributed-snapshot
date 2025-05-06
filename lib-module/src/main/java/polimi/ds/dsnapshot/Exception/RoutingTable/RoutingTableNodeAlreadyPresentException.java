package polimi.ds.dsnapshot.Exception.RoutingTable;

/**
 * Exception stating that we are trying to add to the routing table an entry which is already present.
 * TODO: maybe refactor these exceptions a bit to collapse them into one.
 */
public class RoutingTableNodeAlreadyPresentException extends Exception {
    /**
     * Constructor of the exception.
     */
    public RoutingTableNodeAlreadyPresentException() {
        super("Node is already present in the routing table!");
    }
}
