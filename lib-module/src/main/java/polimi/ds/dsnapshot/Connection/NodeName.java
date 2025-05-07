package polimi.ds.dsnapshot.Connection;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a network node with an IP address and a port.
 * We can assume it is a final class and it is used only in readonly mode.
 */
public class NodeName implements Serializable {

    /**
     * Internal IP.
     */
    private String IP;
    /**
     * Internal port.
     */
    private int port;

    /**
     * Public constructor of the node name.
     * @param ip IP of the node.
     * @param port Port of the node.
     */
    public NodeName(String ip, int port){
        this.IP = ip;
        this.port = port;
    }

    // <editor-fold desc="Getters">

    /**
     * Getter of the node IP.
     * @return String containing the IP.
     */
    public String getIP() {
        return this.IP;
    }

    /**
     * Getter of the node Port.
     * @return Integer containing the port name.
     */
    public int getPort() {
        return this.port;
    }
    // </editor-fold>
    /**
     * Compares this node with another object to check for equality.
     * Two nodes are considered equal if they have the same IP address and port.
     * @param other The object to compare.
     * @return True if the objects are equal.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        NodeName otherNodeName = (NodeName) other;
        return this.port == otherNodeName.port && Objects.equals(this.IP, otherNodeName.IP);
    }

    /**
     * Method to calculate the hash code of the name.
     * @return The integer hash code of the name.
     */
    @Override
    public int hashCode() {
        return Objects.hash(IP, port);
    }
}
