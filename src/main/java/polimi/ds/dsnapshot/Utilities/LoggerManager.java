package polimi.ds.dsnapshot.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.*;

public class LoggerManager {
    private static boolean started = false;
    private static LoggerManager instance;

    private static Boolean mute = Config.getBoolean("snapshot.mute");
    private static String logPath =Config.getString("logger.path");

    private static Logger logger;

    private LoggerManager() {}

    public static void start(int port) {
        logger = Logger.getLogger(Config.getString("logger.loggerName")+ port);

        createDirectory();
        saveOldLog(port);
        init(port);

        started = true;
    }

    public synchronized static LoggerManager getInstance() {
        if (instance == null) {
            instance = new LoggerManager();
        }
        return instance;
    }

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

    public Logger getLogger() {
        if(!started)return null;
        return logger;
    }

    public static Logger instanceGetLogger(){
        return getInstance().getLogger();
    }

    public void mutableInfo(String msg, Optional<String> className, Optional<String> methodName) {
        if(!mute && started){
            String resolvedClass =  className.orElse(this.getClass().getName());
            String resolvedMethod = methodName.orElse("mutableInfo");

            logger.logp(Level.INFO ,resolvedClass,resolvedMethod,msg);
        }
    }


}
