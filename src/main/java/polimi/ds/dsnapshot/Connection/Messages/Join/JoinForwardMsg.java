package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

public class JoinForwardMsg  extends JoinMsg {

    private final String anchorIp;
    private final int anchorPort;

    public JoinForwardMsg(String ip, int port, String anchorIp, int anchorPort) {
        super(MessageID.MESSAGE_JOINFORWARD, ip, port);

        this.anchorIp = anchorIp;
        this.anchorPort = anchorPort;
    }

    public final String getAnchorIp() {
        return anchorIp;
    }

    public final int getAnchorPort() {
        return anchorPort;
    }
}
