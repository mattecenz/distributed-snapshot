package polimi.ds.dsnapshot.Exception;

public class DSNetworkCrashedException extends DSException{
    public DSNetworkCrashedException() {super("Some client in the network crashed. It is not possible to send messages anymore.");}
}
