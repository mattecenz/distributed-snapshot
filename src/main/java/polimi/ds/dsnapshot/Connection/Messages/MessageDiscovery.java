package polimi.ds.dsnapshot.Connection.Messages;

import org.w3c.dom.Node;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message sent whenever someone wants to discovery a new node.
 * Since we work with relatively small networks we can assume that it has an infinite TTl
 */
public class MessageDiscovery extends Message {

    /**
     * Name of the client who created the message
     */
    private final NodeName originName;
    /**
     * Name of the client who needs to be discovered
     */
    private final NodeName destinationName;

    /**
     * Constructor of the message
     * @param originName own name
     * @param destinationName name of node to be discovered
     */
    public MessageDiscovery(NodeName originName, NodeName destinationName) {
        super(MessageID.MESSAGE_DISCOVERY);

        this.originName = originName;
        this.destinationName = destinationName;
    }

    /**
     * Constructor of the message called only by its own subclasses
     * @param messageID ID of the subclass message
     * @param seqNumber Sequence number of the original discovery message
     * @param originName own name
     * @param destinationName name of node to be discovered
     */
    protected MessageDiscovery(MessageID messageID, int seqNumber, NodeName originName, NodeName destinationName) {
        super(messageID, seqNumber);

        this.originName = originName;
        this.destinationName = destinationName;
    }

    /**
     * Getter of the origin name
     * @return origin node name
     */
    public NodeName getOriginName() {
        return this.originName;
    }

    /**
     * Getter of the destination name
     * @return destination node name
     */
    public NodeName getOriginPort() {
        return this.destinationName;
    }
}
