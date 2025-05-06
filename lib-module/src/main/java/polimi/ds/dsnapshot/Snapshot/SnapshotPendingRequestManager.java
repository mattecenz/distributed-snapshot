package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Exception.Snapshot.SnapshotPendingRequestManagerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manager of all the pending requests of the snapshot when waiting for the agreement result from other nodes.
 */
public class SnapshotPendingRequestManager {
    /**
     * List of names who need to answer the request.
     */
    private final List<NodeName> pendingRequests  = new ArrayList<NodeName>();
    /**
     * Optional variable containing the handler who sent initially the request.
     * If it is empty it means that I created the request.
     */
    private final Optional<ClientSocketHandler> SnapshotRequestSender;
    /**
     * Identifier of the snapshot to be restored.
     */
    private final SnapshotIdentifier snapshotIdentifier;
    /**
     * Lock used for synchronization purposes.
     */
    private final Object snapshotLock = new Object();

    /**
     * Constructor of the manager.
     * @param SnapshotRequestSender Optional name of the sender socket.
     * @param snapshotIdentifier Unique identifier of the snapshot to be restored.
     */
    public SnapshotPendingRequestManager(Optional<ClientSocketHandler> SnapshotRequestSender, SnapshotIdentifier snapshotIdentifier) {
        this.SnapshotRequestSender = SnapshotRequestSender;
        this.snapshotIdentifier = snapshotIdentifier;
    }

    /**
     * Method to check if this node is the leader of the snapshot.
     * @param snapshotIdentifier Unique identifier to be checked.
     * @return True if I am the leader of the snapshot.
     * @throws SnapshotPendingRequestManagerException If something goes wrong when contacting the pending request manager.
     *                                                Normally it is caused because the identifier is not valid.
     */
    public boolean isNodeSnapshotLeader(SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        this.snapshotIdentifierComparison(snapshotIdentifier);

        return SnapshotRequestSender.isEmpty();
    }

    /**
     * Method used for adding a new request from a node to the set of pending requests.
     * @param nodeName Name of the added node.
     */
    public void addPendingRequest(NodeName nodeName) {
        pendingRequests.add(nodeName);
    }

    /**
     * TODO: ok I get it the method above can even not be synchronized since it is called only by one thread.
     *      but I am pretty sure that this should be synchronized (In reality it should not give problems BUT theoretically it should).
     * Getter of the number of current pending requests.
     * @return An integer containing the number of pending requests.
     */
    public int pendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * Method to remove a pending request from the queue.
     * When all the pending requests have been consumed, notify the main waiting thread of the snapshot
     * who can proceed with the decision.
     * @param nodeName Name of the node to be removed from the queue.
     * @param snapshotIdentifier Identifier of the snapshot. Useful for error checking.
     * @return True if the pending requests are empty.
     * @throws SnapshotPendingRequestManagerException Generic exception. It could be that the identifier in input does not match the object.
     */
    public boolean removePendingRequest(NodeName nodeName, SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        this.snapshotIdentifierComparison(snapshotIdentifier);

        pendingRequests.remove(nodeName);
        synchronized (snapshotLock) {
            if (pendingRequests.isEmpty()) snapshotLock.notifyAll();
        }
        return pendingRequests.isEmpty();
    }

    /**
     * TODO: also here keeping optional would be very cool.
     * Method to return the original sender of the request for the 2PC.
     * Useful for sending to him the answer of my part of the network.
     * @param snapshotIdentifier Identifier associated to the snapshot.
     * @return The sender of the snapshot, or null.
     * @throws SnapshotPendingRequestManagerException Generic exception. It can be due to the fact that the identifier does not match the saved one.
     */
    public ClientSocketHandler getSnapshotRequestSender(SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        this.snapshotIdentifierComparison(snapshotIdentifier);

        return SnapshotRequestSender.orElse(null);
    }

    /**
     * Method to check if the pending requests are emply.
     * @param snapshotIdentifier Identifier associated to the snapshot.It can be due to the fact that the identifier does not match the saved one.
     * @return True if the list is empty.
     * @throws SnapshotPendingRequestManagerException
     */
    public boolean isEmpty(SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        this.snapshotIdentifierComparison(snapshotIdentifier);

        return pendingRequests.isEmpty();
    }

    /**
     * Method to access the lock of the manager.
     * Useful to the connection manager as the thread will sleep and be notified on it when all the responses arrived.
     * @return The lock of the snapshot manager.
     */
    public Object getSnapshotLock() {
        return snapshotLock;
    }

    /**
     * Method to check the integrity of the snapshot identifier in input.
     * @param snapshotIdentifier Identifier to be checked.
     * @throws SnapshotPendingRequestManagerException If the input identifier does not match the internal one.
     */
    private void snapshotIdentifierComparison(SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        if(!snapshotIdentifier.equals(this.snapshotIdentifier)) throw new SnapshotPendingRequestManagerException("Invalid operation with pending snapshot request, the provided snapshot Identifier does not coincide with the current handled snapshot.");
    }
}
