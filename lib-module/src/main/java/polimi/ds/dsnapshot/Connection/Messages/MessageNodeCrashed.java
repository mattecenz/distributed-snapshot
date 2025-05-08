package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message sent whenever a node has crashed, It stops the execution of the library.
 */
public class MessageNodeCrashed extends Message{

    /**
     * Constructor of the message.
     */
    public MessageNodeCrashed() {
        super(MessageID.MESSAGE_NODE_CRASHED);
    }

}
