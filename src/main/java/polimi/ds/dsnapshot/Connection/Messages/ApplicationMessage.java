package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

public class ApplicationMessage extends Message {
    private final byte[] applicationContent;
    private final NodeName applicationName;
    public ApplicationMessage(byte[] applicationContent, NodeName applicationName, boolean needAck) {
        super(MessageID.MESSAGE_APP, needAck);

        this.applicationContent = applicationContent;
        this.applicationName = applicationName;
    }

    public byte[] getApplicationContent() {
        return applicationContent;
    }

    public NodeName getApplicationName() {
        return applicationName;
    }
}
