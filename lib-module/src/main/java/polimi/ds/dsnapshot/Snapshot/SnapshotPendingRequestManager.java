package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Exception.SnapshotPendingRequestManagerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SnapshotPendingRequestManager {
    private final List<NodeName> pendingRequests  = new ArrayList<NodeName>();
    private final Optional<ClientSocketHandler> SnapshotRequestSender;
    private final SnapshotIdentifier snapshotIdentifier;

    public SnapshotPendingRequestManager(Optional<ClientSocketHandler> SnapshotRequestSender, SnapshotIdentifier snapshotIdentifier) {
        this.SnapshotRequestSender = SnapshotRequestSender;
        this.snapshotIdentifier = snapshotIdentifier;
    }

    public boolean isNodeSnapshotLeader(SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        this.snapshotIdentifierComparison(snapshotIdentifier);

        return SnapshotRequestSender.isEmpty();
    }

    public void addPendingRequest(NodeName nodeName) {
        pendingRequests.add(nodeName);
    }

    public boolean removePendingRequest(NodeName nodeName, SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        this.snapshotIdentifierComparison(snapshotIdentifier);

        pendingRequests.remove(nodeName);
        return pendingRequests.isEmpty();
    }

    public ClientSocketHandler getSnapshotRequestSender(SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        this.snapshotIdentifierComparison(snapshotIdentifier);

        return SnapshotRequestSender.orElse(null);
    }

    private void snapshotIdentifierComparison(SnapshotIdentifier snapshotIdentifier) throws SnapshotPendingRequestManagerException {
        if(!snapshotIdentifier.equals(this.snapshotIdentifier)) throw new SnapshotPendingRequestManagerException("Invalid operation with pending snapshot request, the provided snapshot Identifier does not coincide with the current handled snapshot.");
    }
}
