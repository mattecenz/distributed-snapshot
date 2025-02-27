package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

public class ExitMsg extends Message {
    private char[] newAnchorIp = new char[15];
    private final int newAnchorPort;

    public ExitMsg(char[] newAnchorIp, int newAnchorPort) {
        super(false);

        this.newAnchorIp = newAnchorIp;
        this.newAnchorPort = newAnchorPort;
        this.internalBits = MessageUtility.BIT_EXIT;
    }

    public char[] getNewAnchorIp() {
        return newAnchorIp;
    }
    public int getNewAnchorPort() {
        return newAnchorPort;
    }
}
