package polimi.ds.dsnapshot.Connection.SnashotSerializable;

import java.io.Serializable;

/**
 * Interface used by the serializable parts of the library that need to be saved in the disk when performing a snapshot.
 */
public interface SnapshotSerializable {

    /**
     * Method to check if the current version of the serializable object is compatible with the one read from disk.
     * @param snapshotSerializable Serializable version of the object.
     * @return True if the objects are compatible.
     */
    public abstract boolean serializedValidation(Serializable snapshotSerializable);

    /**
     * Method to get the serializable version of the object.
     * @return The serializable object.
     */
    public abstract Serializable toSerialize();
}
