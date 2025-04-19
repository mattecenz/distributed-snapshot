package polimi.ds.dsnapshot.Exception;

public class DSMessageToMyselfException extends DSException {
    public DSMessageToMyselfException() {
        super("You are trying to send a message to yourself! ");
    }
}
