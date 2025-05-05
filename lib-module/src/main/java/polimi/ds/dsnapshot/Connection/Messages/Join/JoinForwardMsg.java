package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message sent when a parent notifies the network of the entrance of a new node.
 */
public class JoinForwardMsg  extends JoinMsg {

    /**
     * Constructor of the message.
     * @param newNodeName The name of the new node.
     */
    public JoinForwardMsg(NodeName newNodeName) {
        super(MessageID.MESSAGE_JOINFORWARD, newNodeName);
    }
}