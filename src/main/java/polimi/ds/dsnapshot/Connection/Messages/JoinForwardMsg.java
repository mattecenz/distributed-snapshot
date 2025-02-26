package polimi.ds.dsnapshot.Connection.Messages;

public class JoinForwardMsg  extends JoinMsg {

    private char[] anchorIp;
    private int anchorPort;

    public JoinForwardMsg(char[] ip, int port, char[] anchorIp, int anchorPort) {
        super(ip, port);

        this.internalBits = MessageUtility.BIT_JOIN_FORWARD;
    }

    public char[] getAnchorIp() {
        return anchorIp;
    }

    public int getAnchorPort() {
        return anchorPort;
    }
}
