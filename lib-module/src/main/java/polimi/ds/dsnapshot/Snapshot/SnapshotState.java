package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.RoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.SerializableRoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.SPT.SerializableSpanningTree;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.SPT.SpanningTree;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;

import java.io.Serializable;
import java.util.Stack;

public class SnapshotState implements Serializable {
    //private NodeName anchorNode;
    private final SerializableSpanningTree serializableSpanningTree;
    private final SerializableRoutingTable routingTable;

    private final Serializable applicationState;
    private final Stack<CallbackContentWithName> messageInputStack = new Stack<>();

    public SnapshotState(SpanningTree spanningTree, RoutingTable routingTable, Serializable applicationState) {
        this.routingTable = (SerializableRoutingTable)routingTable.toSerialize();
        this.applicationState = applicationState;
        this.serializableSpanningTree = (SerializableSpanningTree) spanningTree.toSerialize();
    }


    public void pushMessage(CallbackContentWithName callbackContentWithName) {
        messageInputStack.push(callbackContentWithName);
    }

    public SerializableSpanningTree getSerializableSpanningTree() {
        return serializableSpanningTree;
    }

    public Stack<CallbackContentWithName> getMessageInputStack() {
        return messageInputStack;
    }
    public Serializable getApplicationState() {
        return applicationState;
    }


    public SerializableRoutingTable getRoutingTable() {
        return routingTable;
    }
}
