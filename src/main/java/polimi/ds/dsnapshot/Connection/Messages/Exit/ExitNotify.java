package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Message;

public class ExitNotify extends Message {
    private char[] exitIp = new char[15];
    private final int exitPort;

    public ExitNotify(char[] exitIp, int exitPort) {
        super(false);

        this.exitIp = exitIp;
        this.exitPort = exitPort;
    }

    public char[] getExitIp() {
        return exitIp;
    }

    public int getExitPort() {
        return exitPort;
    }
}
