package polimi.ds.dsnapshot.Exception;

public class AckHandlerAlreadyRemovedException extends Exception {
    public AckHandlerAlreadyRemovedException() {super("Ack already removed from the list.");}
}
