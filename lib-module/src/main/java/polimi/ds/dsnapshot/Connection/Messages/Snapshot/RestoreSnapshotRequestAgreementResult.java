package polimi.ds.dsnapshot.Connection.Messages.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Snapshot.SnapshotIdentifier;

/**
 * Message which is sent in the 2PC procedure when the leader (person who started the snapshot) takes a decision
 * when all the other nodes have responded.
 * It can either be positive (i.e. we can initiate the restore procedure) or negative.
 */
public class RestoreSnapshotRequestAgreementResult extends RestoreSnapshotRequest{
    /**
     * Boolean which contains the result of the agreement
     */
    private final boolean agreementResult;

    /**
     * Constructor of the message.
     * @param restoreSnapshotResponse response received from the other nodes in the 2PC protocol used.
     */
    public RestoreSnapshotRequestAgreementResult(RestoreSnapshotResponse restoreSnapshotResponse) {
        super(restoreSnapshotResponse.getSnapshotIdentifier(), MessageID.SNAPSHOT_RESET_AGREEMENT);
        this.agreementResult = restoreSnapshotResponse.isSnapshotValid();
    }

    /**
     * Getter of the result of the agreement procedure.
     * @return Boolean with the result of the procedure.
     */
    public boolean getAgreementResult() {
        return agreementResult;
    }
}
