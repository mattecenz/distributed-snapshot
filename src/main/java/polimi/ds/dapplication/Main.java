package polimi.ds.dapplication;

import polimi.ds.dsnapshot.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Exception.JavaDSException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;

import java.util.Scanner;

public class Main {

    /**
     * Scanner to read the input of the user
     */
    private final static Scanner scanner = new Scanner(System.in);

    /**
     * Hook from callback from the library
     */
    private final static AppUtility appUtility = new AppUtility();

    /**
     * Lock of the screen component to avoid weird shenanigans with threads
     */
    private final static Object screenLock = new Object();

    /**
     * Regex to check that the inserted ip is correct
     */
    private final static String regexIp = "|localhost|(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";

    /**
     * Regex for yes/no answers
     */
    private final static String regexYN = "y|Y|n|N";

    /**
     * Static function used to print on the screen in a thread-safe manner
     * @param message message to be print
     */
    private static void printMessageTS(String message){
        synchronized (screenLock){
            System.out.println(message);
        }
    }

    /**
     * Retry the input until the regex is not matched
     * @param regex regex to match
     * @return the correct input string
     */
    private static String retryInput(String regex){
        String input = scanner.nextLine();
        while(!input.trim().matches(regex)){
        printMessageTS("Invalid input, please try again ");
            input = scanner.nextLine();
        }
        return input.trim();
    }

    private static void applicationLoop(){

        while(true){
            // send a message to a certain node

            System.out.print("Enter ip of the receiver of the message: ");
            String ip = retryInput(regexIp);

            System.out.print("Enter port of the receiver of the message: ");
            int port = scanner.nextInt();

            System.out.print("Enter text message to send: ");
            String message = scanner.nextLine();

            // create message
            // TODO: finish the application layer interface
            // JavaDistributedSnapshot.sendMessage();

        }

    }

    private static void joinNetwork(){

        System.out.print("Enter ip address: ");
        String ip = retryInput(regexIp);

        // Avoid error checking on the port for the moment
        System.out.print("Enter port: ");
        int port = scanner.nextInt();

        try {
            JavaDistributedSnapshot.joinNetwork(appUtility, ip, port);
        } catch (JavaDSException e) {
            // The error output stream for the moment can be used without locks
            System.err.println("We caught an exception! "+e.getMessage());
        }

        // At this moment we have a new connection so we can do whatever we want
        applicationLoop();

    }

    private static void createNetwork(){

        System.out.print("Enter port of the client you want to create: ");
        int port = scanner.nextInt();

        // TODO: is it good ?
        JavaDistributedSnapshot.startSocketConnection(port);

        applicationLoop();
    }

    public static void main(String[] args) {
        System.out.println("Hello, World!");

        // Ask the client if he wants to join a network or not
        System.out.print("Do you want to create a new network? [y/n] ");
        String res = retryInput(regexYN);

        if(res.equalsIgnoreCase("y")){
            createNetwork();
        }
        else{
            joinNetwork();
        }
    }
}
