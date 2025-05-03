package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.Objects;

public class SnapshotIdentifier implements Serializable {
    private final NodeName snapshotCreatorName;
    private final String snapshotId;

    public SnapshotIdentifier(NodeName snapshotCreatorName, String snapshotId) {
        this.snapshotCreatorName = snapshotCreatorName;
        this.snapshotId = snapshotId;
    }

    public NodeName getSnapshotCreatorName() {
        return snapshotCreatorName;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotIdentifier that = (SnapshotIdentifier) o;
        return Objects.equals(snapshotCreatorName, that.snapshotCreatorName) &&
                Objects.equals(snapshotId, that.snapshotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotCreatorName, snapshotId);
    }
}
