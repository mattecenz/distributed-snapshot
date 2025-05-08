package polimi.ds.dsnapshot.Snapshot;

import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.RoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.SerializableRoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.SPT.SerializableSpanningTree;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.SPT.SpanningTree;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;

import java.io.Serializable;
import java.util.Stack;

/**
 * Class which wraps the full state of the application saved by the snapshot.
 */
public class SnapshotState implements Serializable {
    //private NodeName anchorNode;
    /**
     * Serializable object containing all the information about the spanning tree.
     */
    private final SerializableSpanningTree serializableSpanningTree;
    /**
     * Serializable object containing all the information about the routing table.
     */
    private final SerializableRoutingTable routingTable;
    /**
     * State of the application.
     */
    private final Serializable applicationState;
    /**
     * TODO: discuss about this. Is it equivalent to have one stack per incoming channel? In theory yes.
     * Stack which contains all the messages received from all the incoming channels in order.
     */
    private final Stack<CallbackContentWithName> messageInputStack = new Stack<>();

    /**
     * Constructor of the state of the application.
     * @param spanningTree Current spanning tree of the library.
     * @param routingTable Current routing table of the library.
     * @param applicationState Current application state.
     */
    public SnapshotState(SpanningTree spanningTree, RoutingTable routingTable, Serializable applicationState) {
        this.routingTable = (SerializableRoutingTable)routingTable.toSerialize();
        this.applicationState = applicationState;
        this.serializableSpanningTree = (SerializableSpanningTree) spanningTree.toSerialize();
    }

    /**
     * Method called when a message that needs to be stored in the snapshot is received.
     * It pushes the message in the stack.
     * @param callbackContentWithName Callback content containing both the message and the channel from which it was received.
     */
    public void pushMessage(CallbackContentWithName callbackContentWithName) {
        messageInputStack.push(callbackContentWithName);
    }

    /**
     * Getter of the saved spanning tree.
     * @return The saved spanning tree.
     */
    public SerializableSpanningTree getSerializableSpanningTree() {
        return serializableSpanningTree;
    }

    /**
     * Getter of the saved messages stack.
     * @return The saved messages stack.
     */
    public Stack<CallbackContentWithName> getMessageInputStack() {
        return messageInputStack;
    }

    /**
     * Getter of the saved application state.
     * @return The saved application state.
     */
    public Serializable getApplicationState() {
        return applicationState;
    }

    /**
     * Getter of the saved routing table.
     * @return The saved routing table.
     */
    public SerializableRoutingTable getRoutingTable() {
        return routingTable;
    }
}
