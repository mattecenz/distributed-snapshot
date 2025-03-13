package polimi.ds.dsnapshot.Connection.Messages;

public class ApplicationMessage extends Message {
    private final byte[] applicationContent;
    private final String applicationIp;
    private final int applicationPort;
    public ApplicationMessage(byte[] applicationContent, String applicationIp, int applicationPort, boolean needAck) {
        super(MessageID.MESSAGE_APP, needAck);

        this.applicationContent = applicationContent;
        this.applicationIp = applicationIp;
        this.applicationPort = applicationPort;
    }

    public byte[] getApplicationContent() {
        return applicationContent;
    }

    public int getApplicationPort() {
        return applicationPort;
    }

    public String getApplicationIp() {
        return applicationIp;
    }
}
