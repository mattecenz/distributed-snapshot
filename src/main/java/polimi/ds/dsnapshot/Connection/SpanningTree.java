package polimi.ds.dsnapshot.Connection;

import polimi.ds.dsnapshot.Exception.SpanningTreeException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpanningTree {
    private ClientSocketHandler anchorNodeHandler;
    private final List<ClientSocketHandler> children = new ArrayList<>();

    public SpanningTree(){
        LoggerManager.getInstance().mutableInfo("create new spanning tree", Optional.of(this.getClass().getName()), Optional.of("SpanningTree"));
    }
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
        LoggerManager.getInstance().mutableInfo("set new anchor node in spanning tree: " + anchorNodeHandler.getRemoteNodeName().getIP() + ":" + anchorNodeHandler.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("setAnchorNodeHandler"));
        this.anchorNodeHandler = anchorNodeHandler;
    }

    protected void addChild(ClientSocketHandler newChild) throws SpanningTreeException {
        LoggerManager.getInstance().mutableInfo("add child to spanning tree: " + newChild.getRemoteNodeName().getIP()+":"+newChild.getRemoteNodeName().getPort(), Optional.of(this.getClass().getName()), Optional.of("addChild"));
        if (children.contains(newChild)) throw new SpanningTreeException("el already in the SPT");
        if (newChild.equals(anchorNodeHandler)) throw new SpanningTreeException("this is not Alabama");
        children.add(newChild);
    }

    protected void RemoveChild(ClientSocketHandler child) throws SpanningTreeException {
        LoggerManager.getInstance().mutableInfo("child removed from the spanning tree: " + child.getRemoteNodeName().getIP()+":"+child.getRemoteNodeName().getIP(), Optional.of(this.getClass().getName()), Optional.of("addChild"));
        if (!children.contains(child)) throw new SpanningTreeException("el already in the SPT");
        children.remove(child);
    }


}
