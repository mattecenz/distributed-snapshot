package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Explicit message sent when a node wants to leave the network
 */
public class ExitMsg extends Message {
    /**
     * Name of the new anchor node to which the client needs to connect
     */
    private final NodeName newAnchorName;

    /**
     * Constructor of the message
     * @param newAnchorName name of the new anchor node
     */
    public ExitMsg(NodeName newAnchorName) {
        super(MessageID.MESSAGE_EXIT, false);

        this.newAnchorName = newAnchorName;
    }

    /**
     * Getter of the new anchor node name
     * @return the anchor node name
     */
    public final NodeName getNewAnchorName() {
        return this.newAnchorName;
    }
}
