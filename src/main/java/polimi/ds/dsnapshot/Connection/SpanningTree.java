package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Exception.SpanningTreeException;

import java.util.ArrayList;
import java.util.List;

public class SpanningTree {
    private ClientSocketHandler anchorNodeHandler;
    private final List<ClientSocketHandler> children = new ArrayList<>();

    public SpanningTree(){}
    // Copy constructor
    public SpanningTree(SpanningTree other) {
        this.anchorNodeHandler = other.anchorNodeHandler;
        this.children.addAll(other.children);
    }


    public ClientSocketHandler getAnchorNodeHandler() {
        return anchorNodeHandler;
    }

    protected List<ClientSocketHandler> getChildren() {
        return children;
    }

    public void setAnchorNodeHandler(ClientSocketHandler anchorNodeHandler) {
        this.anchorNodeHandler = anchorNodeHandler;
    }

    protected void addChild(ClientSocketHandler newChild) throws SpanningTreeException {
        if (children.contains(newChild)) throw new SpanningTreeException("el already in the SPT");
        if (newChild.equals(anchorNodeHandler)) throw new SpanningTreeException("this is not Alabama");
        children.add(newChild);
    }

    protected void RemoveChild(ClientSocketHandler child) throws SpanningTreeException {
        if (!children.contains(child)) throw new SpanningTreeException("el already in the SPT");
        children.remove(child);
    }


}
