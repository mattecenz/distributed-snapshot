package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

import java.io.Serializable;

public class ApplicationMessage extends Message {
    private final Serializable applicationContent;
    private final NodeName applicationName;
    public ApplicationMessage(Serializable applicationContent, NodeName applicationName, boolean needAck) {
        super(MessageID.MESSAGE_APP, needAck);

        this.applicationContent = applicationContent;
        this.applicationName = applicationName;
    }

    public Serializable getApplicationContent() {
        return applicationContent;
    }

    public NodeName getApplicationName() {
        return applicationName;
    }
}
