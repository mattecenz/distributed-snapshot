package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

/**
 * The JoinForwardMsg class represents a forwarded join request message
 * in a distributed network. It extends the {@code JoinMsg} class and includes
 * additional information about the IP and port of an anchor node.
 */
public class JoinForwardMsg  extends JoinMsg {

    /**
     * Ip of the new node who joins the network
     */
    private final String ipNewNode;
    /**
     * Port of the new node who joins the network
     */
    private final int portNewNode;

    /**
     * Constructs a forwarded join message with the specified information.
     * @param ipNewNode The IP address of the anchor node.
     * @param portNewNode The port of the anchor node.
     */
    public JoinForwardMsg(String ipNewNode, int portNewNode) {
        super(MessageID.MESSAGE_JOINFORWARD);

        this.ipNewNode = ipNewNode;
        this.portNewNode = portNewNode;
    }

    /**
     * Returns the IP address of the new node.
     *
     * @return The new node's IP address.
     */
    public final String getIpNewNode() {
        return this.ipNewNode;
    }

    /**
     * Returns the port of the new node.
     *
     * @return The new node's port.
     */
    public final int getPortNewNode() {
        return this.portNewNode;
    }
}