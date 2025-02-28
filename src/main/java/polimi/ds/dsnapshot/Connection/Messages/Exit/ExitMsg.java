package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

public class ExitMsg extends Message {
    private final String newAnchorIp;
    private final int newAnchorPort;

    public ExitMsg(String newAnchorIp, int newAnchorPort) {
        super(MessageID.MESSAGE_EXIT, false);

        this.newAnchorIp = newAnchorIp;
        this.newAnchorPort = newAnchorPort;
    }

    public final String getNewAnchorIp() {
        return newAnchorIp;
    }
    public final int getNewAnchorPort() {
        return newAnchorPort;
    }
}
