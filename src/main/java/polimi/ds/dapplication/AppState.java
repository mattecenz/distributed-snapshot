package polimi.ds.dapplication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppState implements Serializable {
    private final List<String> messageHistory;

    public AppState(){
        this.messageHistory = new ArrayList<>();
    }

    public void appendMessage(String msg){
        this.messageHistory.add(msg);
    }

    public List<String> getMessageHistory(){
        return this.messageHistory;
    }
}
