package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;

public class ExitNotify extends Message {
    private final String exitIp;
    private final int exitPort;

    public ExitNotify(String exitIp, int exitPort) {
        super(MessageID.MESSAGE_EXITNOTIFY, false);

        this.exitIp = exitIp;
        this.exitPort = exitPort;
    }

    public final String getExitIp() {
        return exitIp;
    }

    public final int getExitPort() {
        return exitPort;
    }
}
