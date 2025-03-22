package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * The JoinForwardMsg class represents a forwarded join request message
 * in a distributed network. It extends the {@code JoinMsg} class and includes
 * additional information about the IP and port of an anchor node.
 */
public class JoinForwardMsg  extends JoinMsg {
    /**
     * Name of the new node who joined
     */
    private final NodeName newNodeName;

    /**
     * Constructs a forwarded join message with the specified information.
     * @param newNodeName The name of the new node
     */
    public JoinForwardMsg(NodeName newNodeName) {
        super(MessageID.MESSAGE_JOINFORWARD);

        this.newNodeName = newNodeName;
    }

    /**
     * Returns the name address of the new node.
     *
     * @return the node name
     */
    public NodeName getNewNodeName() {
        return this.newNodeName;
    }
}