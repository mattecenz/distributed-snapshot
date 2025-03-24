package polimi.ds.dsnapshot.Connection.Messages.Join;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.Messages.MessageUtility;
/**
 * A message used to establish a new direct connection, typically employed during the join phase or when the network needs to adapt to changes in its exits.
 */
public class DirectConnectionMsg extends Message {
    public DirectConnectionMsg() {
        super(MessageID.MESSAGE_DIRECTCONNECTION);
    }
}
