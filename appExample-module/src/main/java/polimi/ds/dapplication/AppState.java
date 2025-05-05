package polimi.ds.dapplication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Example class of a state which can be saved in the distributed snapshot.
 * For it to work correctly it needs to implement the interface Serializable.
 */
public class AppState implements Serializable {
    /**
     * List of messages received.
     */
    private final List<String> messageHistory;

    /**
     * Constructor of the application state.
     */
    public AppState(){
        this.messageHistory = new ArrayList<>();
    }

    /**
     * Method to append a message to the internal list.
     * @param msg String message to be saved.
     */
    public synchronized void appendMessage(String msg){
        this.messageHistory.add(msg);
    }

    /**
     * Method to return the internal message history.
     * NB: it is not safe as it does not return a copy of the list, but a reference to it.
     * @return The list of messages received up to this point.
     */
    public List<String> getMessageHistory(){
        return this.messageHistory;
    }

    /**
     * Method which restores the state of the application once a snapshot is invoked.
     * It clears the current state and loads the new one in input.
     * @param state Serializable object which can be safely casted to AppState.
     */
    public void restoreAppState(Serializable state){
        AppState appState = (AppState) state;
        this.messageHistory.clear();
        this.messageHistory.addAll(appState.getMessageHistory());
    }
}
