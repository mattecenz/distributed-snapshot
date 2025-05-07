package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class which represents the unique identifier generated when creating a snapshot.
 */
public class SnapshotIdentifier implements Serializable {
    /**
     * Name of the initiator of the snapshot.
     */
    private final NodeName snapshotCreatorName;
    /**
     * Unique randomly generated id of the snapshot.
     */
    private final String snapshotId;

    /**
     * Constructor of the identifier.
     * @param snapshotCreatorName Name of the creator.
     * @param snapshotId String identifier.
     */
    public SnapshotIdentifier(NodeName snapshotCreatorName, String snapshotId) {
        this.snapshotCreatorName = snapshotCreatorName;
        this.snapshotId = snapshotId;
    }

    /**
     * Getter of the name of the creator.
     * @return The name of the creator of the snapshot.
     */
    public NodeName getSnapshotCreatorName() {
        return snapshotCreatorName;
    }

    /**
     * Getter of the snapshot id.
     * @return The snapshot id.
     */
    public String getSnapshotId() {
        return snapshotId;
    }

    /**
     * Override of the equals method to check if two snapshots have the same identifiers
     * @param o Generic object.
     * @return True if the two objects are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotIdentifier that = (SnapshotIdentifier) o;
        return Objects.equals(snapshotCreatorName, that.snapshotCreatorName) &&
                Objects.equals(snapshotId, that.snapshotId);
    }

    /**
     * Override of the hash code method to return a unique hash code given both the name and the identifier.
     * @return An integer representing the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(snapshotCreatorName, snapshotId);
    }
}
