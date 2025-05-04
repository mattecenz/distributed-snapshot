package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message sent whenever a node has crashed, It stops the execution of the library
 */
public class MessageNodeCrashed extends Message{

    /**
     * Constructor of the message. For the moment it is not important to save the name of the crashed node.
     * If we want we can add it later.
     */
    public MessageNodeCrashed() {
        super(MessageID.MESSAGE_NODE_CRASHED);
    }

}
