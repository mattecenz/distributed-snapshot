package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

public class DirectConnectionMsg extends JoinMsg{
    public DirectConnectionMsg(String ip, int port) {
        super(MessageID.MESSAGE_DIRECTCONNECTION, ip, port);
    }
}
