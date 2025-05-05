package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Join.JoinMsg;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.NodeName;

/**
 * Message used for adoption requests when a parent gracefully leaves the network.
 * It helps the new children to understand to the new anchor they need to connect to.
 */
public class AdoptionRequestMsg extends JoinMsg {
    /**
     * Constructor of the message.
     * @param joinerName Name of the new node to connect.
     */
    public AdoptionRequestMsg(NodeName joinerName) {
        super(MessageID.MESSAGE_ADOPTION_REQUEST,joinerName);
    }
}
