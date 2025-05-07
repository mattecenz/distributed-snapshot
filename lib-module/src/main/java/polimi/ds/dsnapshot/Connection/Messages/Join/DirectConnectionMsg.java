package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * A message used to establish a new direct connection.
 * It is typically employed during the join phase or when the network needs to adapt to changes in its exits.
 */
public class DirectConnectionMsg extends JoinMsg {
    /**
     * Constructor of the message.
     * @param newNodeName Name of the node who wants to establish a direct connection.
     */
    public DirectConnectionMsg(NodeName newNodeName) {
        super(MessageID.MESSAGE_DIRECTCONNECTION, newNodeName);
    }
}
