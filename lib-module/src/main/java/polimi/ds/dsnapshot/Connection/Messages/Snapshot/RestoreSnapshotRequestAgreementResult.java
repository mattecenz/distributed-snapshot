package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Snapshot.SnapshotIdentifier;

public class RestoreSnapshotRequestAgreementResult extends RestoreSnapshotRequest{
    private final boolean agreementResult;

    public RestoreSnapshotRequestAgreementResult(RestoreSnapshotResponse restoreSnapshotResponse) {
        super(restoreSnapshotResponse.getSnapshotIdentifier(), MessageID.SNAPSHOT_RESET_AGREEMENT);
        this.agreementResult = restoreSnapshotResponse.isSnapshotValid();
    }

    public boolean getAgreementResult() {
        return agreementResult;
    }
}
