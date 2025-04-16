package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Represents a join message used for establishing a connection in the distributed snapshot system.
 */
public class JoinMsg extends Message {


    private final NodeName joinerName;

    /**
     * Constructs a JoinMsg with a specified message ID, IP address, and port number.
     *
     * @param messageID The unique identifier for this type of message.
     * @param joinerName Unique name of the node who enters in the network. It corresponds to the ip:port of the server socket listening for connections
     */
    public JoinMsg(MessageID messageID, NodeName joinerName) {
        super(messageID, true);
        this.joinerName = joinerName;
    }

    /**
     * Constructs a JoinMsg with a default message ID for joining operations.
     *
     * @param joinerName Unique name of the node who enters in the network. It corresponds to the ip:port of the server socket listening for connections
     */
    public JoinMsg(NodeName joinerName) {
        this(MessageID.MESSAGE_JOIN, joinerName);
    }

    /**
     * Getter of the name of the node who wants to join the network
     * @return the node name of the joiner
     */
    public NodeName getJoinerName() {
        return this.joinerName;
    }
}