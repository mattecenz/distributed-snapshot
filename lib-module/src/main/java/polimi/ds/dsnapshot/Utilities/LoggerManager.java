package polimi.ds.dsnapshot.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.*;

/**
 * TODO: revisit a bit the implementation of the class.
 * Utility class used by all components of the library to log information for debug purposes.
 */
public class LoggerManager {
    /**
     * Static boolean which asserts if the logger has been started.
     */
    private static boolean started = false;
    /**
     * Static instance of the logger manager. Used as a singleton object.
     */
    private static LoggerManager instance;
    /**
     * Static boolean which specifies is the logger is muted or not.
     */
    private static Boolean mute = Config.getBoolean("logger.mute");
    /**
     * Static string which indicates the path where the log outputs are stored.
     */
    private static String logPath =Config.getString("logger.path");

    /**
     * Static instance of the logger object.
     */
    private static Logger logger;

    /**
     * Default constructor of the class.
     */
    private LoggerManager() {}

    /**
     * Method which starts the logger.
     * It constructs the directory, saves the old log associated to the input port and initiates normal operations.
     * @param port Input port which can be found also in the name of the logger file.
     */
    public synchronized static void start(int port) {
        logger = Logger.getLogger(Config.getString("logger.loggerName")+ port);

        createDirectory();
        saveOldLog(port);
        init(port);

        started = true;
    }

    /**
     * Singleton static method which returns the instance of the logger manager.
     * @return The instance of the manager.
     */
    public synchronized static LoggerManager getInstance() {
        if (instance == null) {
            instance = new LoggerManager();
        }
        return instance;
    }

    /**
     * Method to explicitly create the logging directory if not already present.
     */
    private static void createDirectory() {
        File directory = new File(logPath);
        if(!directory.exists()){
            boolean created = directory.mkdirs();
            if(!created){
                System.err.println("Failed to create directory " + logPath);
                //todo: decide
            }
        }
    }

    /**
     * Method to save the old log into a different file.
     * It checks for an existing file in the input port and renames it.
     * @param port Port associated to the created log file.
     */
    private static void saveOldLog(int port){
        String logFileName = Config.getString("logger.loggerName")+ port + ".log";
        File logFile = new File(logPath, logFileName);
        File oldLogFile = new File(logPath, Config.getString("logger.loggerName")+".old.log");

        if (logFile.exists()) {
            try {
                Files.move(logFile.toPath(), oldLogFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                System.err.println("Failed to rename log file: " + e.getMessage());
                //todo: decide
            }
        }
    }

    /**
     * Method called to initiate normal operations of the logger.
     * @param port Port associated to the created log file.
     */
    private static void init(int port){
        FileHandler fh;
        logger.setLevel(Level.ALL);
        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler(logPath+"/"+Config.getString("logger.loggerName")+ port +".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // Disable parent handlers
            logger.setUseParentHandlers(Config.getBoolean("logger.showLogInSOut"));
        } catch (SecurityException | IOException  e) {
            System.err.println(e.toString());
            //TODO
        }
    }

    /**
     * Method to retrieve the logger directly.
     * @return The logger if the application has started, else null.
     */
    public synchronized Logger getLogger() {
        if(!started)return null;
        return logger;
    }

    /**
     * Another method to retrieve directly the logger.
     * @return The logger if the application has started, else null.
     */
    public static Logger instanceGetLogger(){
        return getInstance().getLogger();
    }

    /**
     * Method to save directly a string message to the logger.
     * @param msg String message to be saved in the logger.
     * @param className Optional name of the calling class.
     * @param methodName Optional name of the calling method.
     */
    public synchronized  void mutableInfo(String msg, Optional<String> className, Optional<String> methodName) {
        if(!mute && started){
            String resolvedClass =  className.orElse(this.getClass().getName());
            String resolvedMethod = methodName.orElse("mutableInfo");

            logger.logp(Level.INFO ,resolvedClass,resolvedMethod,msg);
        }
    }


}
