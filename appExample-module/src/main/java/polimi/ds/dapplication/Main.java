package polimi.ds.dapplication;

import polimi.ds.dapplication.Message.StringMessage;
import polimi.ds.dsnapshot.Exception.ExportedException.JavaDSException;
import polimi.ds.dsnapshot.Api.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Exception.ExportedException.SnapshotRestoreLocalException;
import polimi.ds.dsnapshot.Exception.ExportedException.SnapshotRestoreRemoteException;

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
    private final static String regexYN = "y|Y|n|N|";

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

    /**
     * Retry the integer input until the user inserts the correct one
     * @return the correct input integer
     */
    private static Integer retryInputInteger(){
        Integer input;
        while(!scanner.hasNextInt()){
            // Flush the number
            scanner.nextLine();
            SystemOutTS.print("Invalid input, please try again: ");
        }
        input = scanner.nextInt();
        // Flush
        scanner.nextLine();
        return input;
    }


    private static void sendMessage(){
        SystemOutTS.print("Enter ip of the receiver of the message: ");
        String ip = retryInput(regexIp);

        SystemOutTS.print("Enter port of the receiver of the message: ");
        int port = retryInputInteger();

        SystemOutTS.print("Enter text message to send: ");
        String message = scanner.nextLine();

        StringMessage sm = new StringMessage(message);

        try {
            JavaDistributedSnapshot.getInstance().sendMessage(sm, ip, port);
        } catch (IOException e) {
            System.err.println("The library threw an IOException: " + e.getMessage());
        }
    }

    private static void applicationLoop(){

        boolean finished = false;

        while(!finished){
            // send a message to a certain node

            SystemOutTS.print("Input the command you want to run:");

            String command = scanner.nextLine();

            if(command.startsWith("/")){
                switch (command.toLowerCase().substring(1)){
                    case "msg" -> {
                        sendMessage();
                    }
                    case "exit" -> {
                        // TODO is this correct ?
                        try {
                            JavaDistributedSnapshot.getInstance().leaveNetwork();
                        } catch (JavaDSException e) {
                            System.err.println("The library threw a JavaDSException: " + e.getMessage());
                        }
                        finished=true;
                    }
                    case "snapshot" ->{
                        // Is there something else to do ? idk
                        JavaDistributedSnapshot.getInstance().startNewSnapshot();
                    }
                    case "restore" ->{
                        restoreSnapshot();
                    }
                    case "surprise" ->{
                        surprise();
                    }
                    case "history" ->{
                        SystemOutTS.println("Full history of messages received: ");
                        for(String m: appState.getMessageHistory()) {
                            // NB: this is not a synchronized operation, so something weird can potentially happen
                            SystemOutTS.println(m);
                        }
                    }
                    case "help" -> {
                        SystemOutTS.println("All commands are: \n" +
                                "msg:\t\t send a message to a user of the network \n" +
                                "exit:\t\t exit the network and the program \n" +
                                "snapshot:\t manually start a snapshot \n" +
                                "history:\t list all received messages \n" +
                                "surprise:\t are you brave enough to discover it? \n"+
                                "");
                    }
                    case null, default -> {
                        SystemOutTS.println("Invalid command, please try again...");
                    }
                }
            }
            else{
                SystemOutTS.println("Invalid command. The right syntax is \"/<command>\" ");
            }

        }

    }

    private static void restoreSnapshot(){
        SystemOutTS.print("Enter the code of the snapshot to be restored: "); //code -> random string
        String code = scanner.nextLine();
        SystemOutTS.print("Enter the ip saved in the name of the snapshot to be restored: ");
        String ip = retryInput(regexIp);
        SystemOutTS.print("Enter port of the snapshot to be restored: ");
        int port = retryInputInteger();

        try {
            JavaDistributedSnapshot.getInstance().restoreSnapshot(code,ip,port);
        } catch (SnapshotRestoreRemoteException e) {
            SystemOutTS.println("The library threw a SnapshotRestoreRemoteException: " + e.getMessage());
        } catch (SnapshotRestoreLocalException e) {
            SystemOutTS.println("The library threw a SnapshotRestoreLocalException: " + e.getMessage());
        }

        SystemOutTS.print("Snapshot restored successfully. can resume operations");
    }

    private static void joinNetwork(){
        SystemOutTS.print("Enter the port you want to open your connection: ");
        int myPort = retryInputInteger();

        // TODO: is it good ?
        JavaDistributedSnapshot.getInstance().startSocketConnection(myPort, appUtility);

        SystemOutTS.print("Enter ip of the node you want to connect to: ");
        String ip = retryInput(regexIp);

        // Avoid error checking on the port for the moment
        SystemOutTS.print("Enter port of the node you want to connect to: ");
        int port = retryInputInteger();

        try {
            JavaDistributedSnapshot.getInstance().joinNetwork(ip, port);

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
        int myPort = retryInputInteger();

        // TODO: is it good ?
        JavaDistributedSnapshot.getInstance().startSocketConnection(myPort, appUtility);

        applicationLoop();
    }

    public static void main(String[] args) {

        // Ask the client if he wants to join a network or not
        SystemOutTS.print("Do you want to create a new network? [y/N] ");
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

    private static void surprise(){
        SystemOutTS.println("""
                ⢀⡴⠑⡄⠀⠀⠀⠀⠀⠀⠀⣀⣀⣤⣤⣤⣀⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
                ⠸⡇⠀⠿⡀⠀⠀⠀⣀⡴⢿⣿⣿⣿⣿⣿⣿⣿⣷⣦⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠑⢄⣠⠾⠁⣀⣄⡈⠙⣿⣿⣿⣿⣿⣿⣿⣿⣆⠀⠀⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⢀⡀⠁⠀⠀⠈⠙⠛⠂⠈⣿⣿⣿⣿⣿⠿⡿⢿⣆⠀⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⢀⡾⣁⣀⠀⠴⠂⠙⣗⡀⠀⢻⣿⣿⠭⢤⣴⣦⣤⣹⠀⠀⠀⢀⢴⣶⣆
                ⠀⠀⢀⣾⣿⣿⣿⣷⣮⣽⣾⣿⣥⣴⣿⣿⡿⢂⠔⢚⡿⢿⣿⣦⣴⣾⠁⠸⣼⡿
                ⠀⢀⡞⠁⠙⠻⠿⠟⠉⠀⠛⢹⣿⣿⣿⣿⣿⣌⢤⣼⣿⣾⣿⡟⠉⠀⠀⠀⠀⠀
                ⠀⣾⣷⣶⠇⠀⠀⣤⣄⣀⡀⠈⠻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⠀⠀⠀⠀⠀⠀
                ⠀⠉⠈⠉⠀⠀⢦⡈⢻⣿⣿⣿⣶⣶⣶⣶⣤⣽⡹⣿⣿⣿⣿⡇⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠉⠲⣽⡻⢿⣿⣿⣿⣿⣿⣿⣷⣜⣿⣿⣿⡇⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⢸⣿⣿⣷⣶⣮⣭⣽⣿⣿⣿⣿⣿⣿⣿⠀⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⣀⣀⣈⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠇⠀⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠃⠀⠀⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠹⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠛⠻⠿⠿⠿⠿⠛⠉
                """);
    }
}
