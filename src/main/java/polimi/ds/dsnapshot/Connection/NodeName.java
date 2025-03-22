package polimi.ds.dsnapshot.Connection;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a network node with an IP address and a port.
 * We can assume it is a final class and it is used only in readonly mode.
 */
public class NodeName implements Serializable {

    /**
     * Internal Ip
     */
    private String IP;
    /**
     * Internal port
     */
    private int port;

    /**
     * Public constructor of the node name
     * @param ip ip of the node
     * @param port port of the node
     */
    public NodeName(String ip, int port){
        this.IP = ip;
        this.port = port;
    }

    // <editor-fold desc="Getters">

    /**
     * Getter of the node IP
     * @return string containing the IP
     */
    public String getIP() {
        return this.IP;
    }

    /**
     * Getter of the node Port
     * @return integer containing the port name
     */
    public int getPort() {
        return this.port;
    }
    // </editor-fold>
    /**
     * Compares this node with another object to check for equality.
     * Two nodes are considered equal if they have the same IP address and port.
     *
     * @param other the object to compare.
     * @return {@code true} if the two nodes have the same IP address and port,
     *         {@code false} otherwise.
     */
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        NodeName otherNodeName = (NodeName) other;
        return this.port == otherNodeName.port && Objects.equals(this.IP, otherNodeName.IP);
    }
}
