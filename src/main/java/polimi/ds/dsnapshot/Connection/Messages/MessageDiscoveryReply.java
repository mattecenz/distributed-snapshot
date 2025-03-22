package polimi.ds.dsnapshot.Connection.Messages;

/**
 * Message created and sent back whenever a node has been discovered
 */
public class MessageDiscoveryReply extends MessageDiscovery{
    public MessageDiscoveryReply(int seqNumber, String originIp, int originPort, String destinationIp, int destinationPort){
        super(MessageID.MESSAGE_DISCOVERYREPLY, seqNumber, originIp, originPort, destinationIp, destinationPort);
    }
}
