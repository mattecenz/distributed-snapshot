package polimi.ds.dsnapshot;

public interface ApplicationLayerInterface {
    
    public abstract void receiveMessage(byte[] messageContent);
    public abstract byte[] getApplicationState();
}
