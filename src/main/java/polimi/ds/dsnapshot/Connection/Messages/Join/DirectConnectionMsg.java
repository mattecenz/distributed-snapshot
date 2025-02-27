package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

public class DirectConnectionMsg extends JoinMsg{
    public DirectConnectionMsg(char[] ip, int port) {
        super(ip, port);
        this.internalBits = MessageUtility.BIT_DIRECT_CONNECTION;
    }
}
