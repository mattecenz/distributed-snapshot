package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;

//TODO: è abstract la classe oppure no ?
public class JoinMsg extends Message {
    private final String ip;
    private final int port;

    public JoinMsg(MessageID messageID, String ip, int port) {
        super(messageID, true);

        this.ip = ip;
        this.port = port;
    }

    public JoinMsg(String ip, int port) {
        this(MessageID.MESSAGE_JOIN, ip, port);
    }

    public final String getIp() {
        return ip;
    }

    public final int getPort() {
        return port;
    }
}
