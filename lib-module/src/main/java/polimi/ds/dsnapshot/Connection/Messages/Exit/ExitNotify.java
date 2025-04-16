package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message created when we want to notify that a particular node has left the network
 */
public class ExitNotify extends Message {
    /**
     * Name of the node who left the network
     */
    private final NodeName exitName;

    /**
     * Constructor of the message
     * @param exitName name of the node
     */
    public ExitNotify(NodeName exitName) {
        super(MessageID.MESSAGE_EXITNOTIFY, false);

        this.exitName = exitName;
    }

    /**
     * Getter of the node name
     * @return the name of the node who left the network
     */
    public final NodeName getExitName() {
        return this.exitName;
    }
}
