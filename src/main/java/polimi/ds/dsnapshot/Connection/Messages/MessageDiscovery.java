package polimi.ds.dsnapshot.Connection.Messages;

/**
 * Message sent whenever someone wants to discovery a new node.
 * Since we work with relatively small networks we can assume that it has an infinite TTl
 */
public class MessageDiscovery extends Message {

    /**
     * Ip of the client who created the message
     */
    private final String originIP;
    /**
     * Port of the client who created the message
     */
    private final int originPort;
    /**
     * Ip of the client who needs to be discovered
     */
    private final String destinationIP;
    /**
     * Port of the client who needs to be discovered
     */
    private final int destinationPort;

    /**
     * Constructor of the message
     * @param originIP own Ip
     * @param originPort own Port
     * @param destinationIP ip to be discovered
     * @param destinationPort port to be discovered
     */
    public MessageDiscovery(String originIP, int originPort, String destinationIP, int destinationPort) {
        super(MessageID.MESSAGE_DISCOVERY);

        this.originIP = originIP;
        this.originPort = originPort;
        this.destinationIP = destinationIP;
        this.destinationPort = destinationPort;
    }

    /**
     * Constructor of the message called only by its own subclasses
     * @param messageID ID of the subclass message
     * @param seqNumber Sequence number of the original discovery message
     * @param originIP own Ip
     * @param originPort own Port
     * @param destinationIP ip to be discovered
     * @param destinationPort port to be discovered
     */
    protected MessageDiscovery(MessageID messageID, int seqNumber, String originIP, int originPort, String destinationIP, int destinationPort) {
        super(messageID, seqNumber);

        this.originIP = originIP;
        this.originPort = originPort;
        this.destinationIP = destinationIP;
        this.destinationPort = destinationPort;
    }

    /**
     * Getter of the origin Ip
     * @return ip
     */
    public String getOriginIP() {
        return this.originIP;
    }

    /**
     * Getter of the origin port
     * @return port
     */
    public int getOriginPort() {
        return this.originPort;
    }

    /**
     * Getter of the destination Ip
     * @return ip
     */
    public String getDestinationIP() {
        return this.destinationIP;
    }

    /**
     * Getter of the destination port
     * @return port
     */
    public int getDestinationPort() {
        return this.destinationPort;
    }
}
