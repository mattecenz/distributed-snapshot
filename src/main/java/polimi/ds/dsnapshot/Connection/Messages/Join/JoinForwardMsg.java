package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

/**
 * The JoinForwardMsg class represents a forwarded join request message
 * in a distributed network. It extends the {@code JoinMsg} class and includes
 * additional information about the IP and port of an anchor node.
 */
public class JoinForwardMsg  extends JoinMsg {

    private final String anchorIp;
    private final int anchorPort;

    /**
     * Constructs a forwarded join message with the specified information.
     *
     * @param ip The IP address of the sender node.
     * @param port The port of the sender node.
     * @param anchorIp The IP address of the anchor node.
     * @param anchorPort The port of the anchor node.
     */
    public JoinForwardMsg(String ip, int port, String anchorIp, int anchorPort) {
        super(MessageID.MESSAGE_JOINFORWARD, ip, port);

        this.anchorIp = anchorIp;
        this.anchorPort = anchorPort;
    }

    /**
     * Returns the IP address of the anchor node.
     *
     * @return The anchor node's IP address.
     */
    public final String getAnchorIp() {
        return anchorIp;
    }

    /**
     * Returns the port of the anchor node.
     *
     * @return The anchor node's port.
     */
    public final int getAnchorPort() {
        return anchorPort;
    }
}
