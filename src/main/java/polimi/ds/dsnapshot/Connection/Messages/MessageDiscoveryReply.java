package polimi.ds.dsnapshot.Connection.Messages;

import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message created and sent back whenever a node has been discovered
 */
public class MessageDiscoveryReply extends MessageDiscovery{
    public MessageDiscoveryReply(int seqNumber, NodeName originName, NodeName destinationName){
        super(MessageID.MESSAGE_DISCOVERYREPLY, seqNumber, originName, destinationName);
    }
}
