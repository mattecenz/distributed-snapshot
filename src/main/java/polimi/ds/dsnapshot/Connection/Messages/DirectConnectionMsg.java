package polimi.ds.dsnapshot.Connection.Messages;

public class DirectConnectionMsg extends JoinMsg{
    public DirectConnectionMsg(char[] ip, int port) {
        super(ip, port);
        this.internalBits = MessageUtility.BIT_DIRECT_CONNECTION;
    }
}
