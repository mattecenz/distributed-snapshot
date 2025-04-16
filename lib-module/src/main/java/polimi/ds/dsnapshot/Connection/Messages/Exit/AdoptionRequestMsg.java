package polimi.ds.dsnapshot.Connection.Messages.Exit;

import polimi.ds.dsnapshot.Connection.Messages.Join.JoinMsg;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageID;
import polimi.ds.dsnapshot.Connection.NodeName;

public class AdoptionRequestMsg extends JoinMsg {
    public AdoptionRequestMsg(NodeName joinerName) {
        super(MessageID.MESSAGE_ADOPTION_REQUEST,joinerName);
    }
}
