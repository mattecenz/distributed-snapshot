package polimi.ds.dsnapshot.Connection;

import java.util.Objects;

/**
 * Represents a network node with an IP address and a port.
 * The class provides getters, setters, and methods to compare instances (equals and hashCode).
 */
public class NetNode {
    private String IP;
    private int port;

    public NetNode(String ip, int port){
        this.IP = ip;
        this.port = port;
    }

    // <editor-fold desc="Getter and Setter">
    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

        NetNode otherNetNode = (NetNode) other;
        return port == otherNetNode.port && Objects.equals(IP, otherNetNode.IP);
    }
}
