package polimi.ds.dsnapshot.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.*;

public class LoggerManager {
    private static LoggerManager instance;

    private static Boolean mute = Config.getBoolean("snapshot.mute");
    private static String logPath =Config.getString("logger.path");

    private static final Logger logger = Logger.getLogger(Config.getString("logger.loggerName"));

    private LoggerManager() {}

    public synchronized static LoggerManager getInstance() {
        if (instance == null) {
            instance = new LoggerManager();

            createDirectory();
            init();
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

    private static void init(){
        FileHandler fh;
        logger.setLevel(Level.ALL);
        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler(logPath+"/"+Config.getString("logger.loggerName")+".log");
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
        return logger;
    }

    public void mutableInfo(String msg, Optional<String> className, Optional<String> methodName) {
        if(!mute){
            String resolvedClass =  className.orElse(this.getClass().getName());
            String resolvedMethod = methodName.orElse("mutableInfo");

            LogRecord record = new LogRecord(Level.INFO, msg);
            record.setSourceClassName(resolvedClass);
            record.setSourceMethodName(resolvedMethod);
            logger.log(record);
        }
    }


}
