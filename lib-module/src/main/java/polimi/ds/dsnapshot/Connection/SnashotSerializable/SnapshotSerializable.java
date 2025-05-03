package polimi.ds.dsnapshot.Connection.SnashotSerializable;

import java.io.Serializable;

public interface SnapshotSerializable {

    public abstract boolean serializedValidation(Serializable snapshotSerializable);
    public abstract Serializable toSerialize();
}
