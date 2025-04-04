package polimi.ds.dsnapshot;

import java.io.Serializable;

public interface ApplicationLayerInterface {
    
    public abstract void receiveMessage(Serializable messageContent);
    public abstract <T extends Serializable> T getApplicationState();
}
