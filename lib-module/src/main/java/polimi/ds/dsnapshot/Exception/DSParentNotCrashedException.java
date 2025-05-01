package polimi.ds.dsnapshot.Exception;

public class DSParentNotCrashedException extends DSException {
    public DSParentNotCrashedException() {super("The parent has not crashed, no need to reconnect.");}
}
