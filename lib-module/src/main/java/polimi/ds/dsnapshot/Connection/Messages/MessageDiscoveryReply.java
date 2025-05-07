package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message created and sent back whenever a node has been discovered.
 */
public class MessageDiscoveryReply extends MessageDiscovery{
    /**
     * Constructor of the message
     * @param seqNumber Sequence number of the original discovery message.
     * @param originName Own name.
     * @param destinationName Name of the destination of the reply.
     */
    public MessageDiscoveryReply(int seqNumber, NodeName originName, NodeName destinationName){
        super(MessageID.MESSAGE_DISCOVERYREPLY, seqNumber, originName, destinationName);
    }
}
