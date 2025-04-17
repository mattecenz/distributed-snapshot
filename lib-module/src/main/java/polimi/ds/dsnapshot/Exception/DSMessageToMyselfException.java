package polimi.ds.dsnapshot.Exception;

public class DSMessageToMyselfException extends Exception {
    public DSMessageToMyselfException() {
        super("You are trying to send a message to yourself! ");
    }
}
