package polimi.ds.dsnapshot;

import java.io.Serializable;

public interface ApplicationLayerInterface {
    
    public abstract <T extends Serializable> void receiveMessage(T messageContent);
    public abstract <T extends Serializable> T getApplicationState();
}
