package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Exception.SpanningTreeChildAlreadyPresentException;
import polimi.ds.dsnapshot.Exception.SpanningTreeChildNotPresentException;
import polimi.ds.dsnapshot.Exception.SpanningTreeNoAnchorNodeException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class which represents the spanning tree of the network from the pov of a single node
 */
public class SpanningTree {
    /**
     * Optional object containing the anchor node.
     * If it is empty it means that either the node has not connected yet to a network or is the node who created the network
     */
    private Optional<ClientSocketHandler> anchorNodeHandler;
    /**
     * List of children socket handlers
     */
    private final List<ClientSocketHandler> children = new ArrayList<>();

    /**
     * Default constructor of the spanning tree. The anchor node is an empty object
     */
    public SpanningTree(){
        LoggerManager.getInstance().mutableInfo("create new spanning tree", Optional.of(this.getClass().getName()), Optional.of("SpanningTree"));
        this.anchorNodeHandler = Optional.empty();
    }

    /**
     * Copy constructor of the spanning tree. It is used for recovery from a snapshot
     * @param other spanning tree object to be copied
     */
    public SpanningTree(SpanningTree other) {
        this.anchorNodeHandler = other.anchorNodeHandler;
        this.children.addAll(other.children);
    }

    /**
     * Getter of the anchor node.
     * It is an atomic operation.
     * @return the anchor node
     * @throws SpanningTreeNoAnchorNodeException if no anchor node is present
     */
    public synchronized ClientSocketHandler getAnchorNodeHandler() throws SpanningTreeNoAnchorNodeException{
        if(this.anchorNodeHandler.isEmpty()) throw new SpanningTreeNoAnchorNodeException();

        return this.anchorNodeHandler.get();
    }

    /**
     * Getter of the list of children
     * @return
     */
    protected List<ClientSocketHandler> getChildren() {
        return this.children;
    }

    /**
     * Set a new anchor node for this node.
     * It is an atomic operation
     * @param anchorNodeHandler client socket handler of the new anchor node
     */
    public synchronized void setAnchorNodeHandler(ClientSocketHandler anchorNodeHandler) {
        LoggerManager.getInstance().mutableInfo("set new anchor node in spanning tree: " + anchorNodeHandler.getRemoteNodeName().getIP() + ":" + anchorNodeHandler.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("setAnchorNodeHandler"));
        this.anchorNodeHandler = Optional.of(anchorNodeHandler);
    }

    /**
     * Remove the current anchor of the node.
     * It is an atomic operation
     * @throws SpanningTreeNoAnchorNodeException if no parent is present
     */
    public synchronized void removeAnchorNodeHandler() throws SpanningTreeNoAnchorNodeException{
        LoggerManager.getInstance().mutableInfo("Removing the current anchor of the node.", Optional.of(this.getClass().getName()), Optional.of("setAnchorNodeHandler"));
        if(this.anchorNodeHandler.isEmpty()) throw new SpanningTreeNoAnchorNodeException();
        this.anchorNodeHandler=Optional.empty();
    }

    /**
     * Add a new child in the spt.
     * It is an atomic operation
     * @param newChild new client socket handler of the child
     * @throws SpanningTreeChildAlreadyPresentException if the child is already present in the spt
     */
    protected synchronized void addChild(ClientSocketHandler newChild) throws SpanningTreeChildAlreadyPresentException {
        LoggerManager.getInstance().mutableInfo("add child to spanning tree: " + newChild.getRemoteNodeName().getIP()+":"+newChild.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("addChild"));
        if (this.children.contains(newChild)) throw new SpanningTreeChildAlreadyPresentException();
        this.children.add(newChild);
    }

    /**
     * Remove a child from the spt.
     * It is an atomic operation
     * @param child socket handler to remove
     * @throws SpanningTreeChildNotPresentException if no child is present in the current list of children
     */
    protected synchronized void removeChild(ClientSocketHandler child) throws SpanningTreeChildNotPresentException {
        LoggerManager.getInstance().mutableInfo("child removed from the spanning tree: " + child.getRemoteNodeName().getIP()+":"+child.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("addChild"));
        if (!this.children.contains(child)) throw new SpanningTreeChildNotPresentException();
        this.children.remove(child);
    }

    public synchronized boolean isNodeLeaf(){
        if(this.anchorNodeHandler.isEmpty() && this.children.size() == 1) return true;
        return this.children.isEmpty();
    }
}
