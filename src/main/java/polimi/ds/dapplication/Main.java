package polimi.ds.dapplication;

import polimi.ds.dapplication.Message.StringMessage;
import polimi.ds.dsnapshot.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Exception.JavaDSException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;

import java.io.IOException;
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
     * Internal state of the application
     */
    private final static AppState appState = new AppState();

    /**
     * Regex to check that the inserted ip is correct
     */
    private final static String regexIp = "|localhost|(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";

    /**
     * Regex for yes/no answers
     */
    private final static String regexYN = "y|Y|n|N";

    /**
     * Retry the input until the regex is not matched
     * @param regex regex to match
     * @return the correct input string
     */
    private static String retryInput(String regex){
        String input = scanner.nextLine();
        while(!input.trim().matches(regex)){
        SystemOutTS.println("Invalid input, please try again ");
            input = scanner.nextLine();
        }
        return input.trim();
    }

    private static void applicationLoop(){

        while(true){
            // send a message to a certain node

            SystemOutTS.print("Enter ip of the receiver of the message: ");
            String ip = retryInput(regexIp);

            SystemOutTS.print("Enter port of the receiver of the message: ");
            int port = scanner.nextInt();

            SystemOutTS.print("Enter text message to send: ");
            String message = scanner.nextLine();

            StringMessage sm = new StringMessage(message);

            try {
                JavaDistributedSnapshot.getInstance().sendMessage(sm, false, ip, port);
            } catch (IOException e) {
                System.err.println("The library threw an IOException: " + e.getMessage());
            }

        }

    }

    private static void joinNetwork(){

        SystemOutTS.print("Enter ip address: ");
        String ip = retryInput(regexIp);

        // Avoid error checking on the port for the moment
        SystemOutTS.print("Enter port: ");
        int port = scanner.nextInt();

        try {
            JavaDistributedSnapshot.getInstance().joinNetwork(appUtility, ip, port);

            // At this moment we have a new connection so we can do whatever we want
            applicationLoop();

            // TODO: add more detailed exceptions
        } catch (JavaDSException e) {
            // The error output stream for the moment can be used without locks
            System.err.println("We caught an exception! "+e.getMessage());
        }

        // Exit from the application if an exception is raised
    }

    private static void createNetwork(){

        SystemOutTS.print("Enter port of the client you want to create: ");
        int port = scanner.nextInt();

        // TODO: is it good ?
        JavaDistributedSnapshot.getInstance().startSocketConnection(port);

        applicationLoop();
    }

    public static void main(String[] args) {
        SystemOutTS.println("Hello, World!");

        // Ask the client if he wants to join a network or not
        SystemOutTS.print("Do you want to create a new network? [y/n] ");
        String res = retryInput(regexYN);

        if(res.equalsIgnoreCase("y")){
            createNetwork();
        }
        else{
            joinNetwork();
        }
    }

    public static AppState getAppState(){
        return appState;
    }
}
