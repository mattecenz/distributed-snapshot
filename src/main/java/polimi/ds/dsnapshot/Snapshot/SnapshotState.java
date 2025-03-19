package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.NetNode;
import polimi.ds.dsnapshot.Connection.RoutingTable;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContent;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class SnapshotState implements Serializable {
    private NetNode anchorNode;
    private RoutingTable routingTable;

    private byte[] applicationState;
    private Stack<CallbackContentWithName> messageInputStack = new Stack<>();

    public SnapshotState(NetNode anchorNode, RoutingTable routingTable, byte[] applicationState) {
        this.anchorNode = anchorNode;
        this.routingTable = routingTable;
        this.applicationState = applicationState;
    }

    public void pushMessage(CallbackContentWithName callbackContentWithName) {
        messageInputStack.push(callbackContentWithName);
    }


    public Stack<CallbackContentWithName> getMessageInputStack() {
        return messageInputStack;
    }
    public byte[] getApplicationState() {
        return applicationState;
    }
}
