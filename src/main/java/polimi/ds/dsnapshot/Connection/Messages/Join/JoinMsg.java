package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;

/**
 * Represents a join message used for establishing a connection in the distributed snapshot system.
 * This message contains the IP address and port number of the joining entity.
 */
public class JoinMsg extends Message {
    private final String ip;
    private final int port;

    /**
     * Constructs a JoinMsg with a specified message ID, IP address, and port number.
     *
     * @param messageID The unique identifier for this type of message.
     * @param ip        The IP address of the joining entity.
     * @param port      The port number of the joining entity.
     */
    public JoinMsg(MessageID messageID, String ip, int port) {
        super(messageID, true);

        this.ip = ip;
        this.port = port;
    }

    /**
     * Constructs a JoinMsg with a default message ID for joining operations.
     *
     * @param ip   The IP address of the joining entity.
     * @param port The port number of the joining entity.
     */
    public JoinMsg(String ip, int port) {
        this(MessageID.MESSAGE_JOIN, ip, port);
    }

    /**
     * Returns the IP address associated with this join message.
     *
     * @return The IP address as a string.
     */
    public final String getIp() {
        return ip;
    }

    /**
     * Returns the port number associated with this join message.
     *
     * @return The port number as an integer.
     */
    public final int getPort() {
        return port;
    }
}
