package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Connection.RoutingTable.RoutingTable;
import polimi.ds.dsnapshot.Connection.RoutingTable.SerializableRoutingTable;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;

import java.io.Serializable;
import java.util.Stack;

public class SnapshotState implements Serializable {
    private NodeName anchorNode;
    private final SerializableRoutingTable routingTable;

    private final Serializable applicationState;
    private final Stack<CallbackContentWithName> messageInputStack = new Stack<>();

    public SnapshotState(NodeName anchorNode, RoutingTable routingTable, Serializable applicationState) {
        this(routingTable, applicationState);
        this.anchorNode = anchorNode;
    }

    public SnapshotState(RoutingTable routingTable, Serializable applicationState){
        this.routingTable = routingTable.toSerialize();
        this.applicationState = applicationState;
        // A bit ugly but cannot use optionals
        this.anchorNode = null;
    }

    public void pushMessage(CallbackContentWithName callbackContentWithName) {
        messageInputStack.push(callbackContentWithName);
    }


    public Stack<CallbackContentWithName> getMessageInputStack() {
        return messageInputStack;
    }
    public Serializable getApplicationState() {
        return applicationState;
    }

    public NodeName getAnchorNode() {
        return anchorNode;
    }

    public SerializableRoutingTable getRoutingTable() {
        return routingTable;
    }
}
