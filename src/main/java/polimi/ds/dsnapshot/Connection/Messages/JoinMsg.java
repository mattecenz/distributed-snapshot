package polimi.ds.dsnapshot.Connection.Messages;

import java.util.Arrays;

public class JoinMsg extends Message{
    private char[] ip = new char[15];
    private int port;
    public JoinMsg(char[] ip, int port) {
        super(true);

        this.ip = ip;
        this.port = port;

        this.internalBits += MessageUtility.BIT_JOIN;
    }

    public char[] getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
