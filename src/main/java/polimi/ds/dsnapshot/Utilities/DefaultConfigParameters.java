package polimi.ds.dsnapshot.Utilities;

import java.util.HashMap;
import java.util.Map;

public class DefaultConfigParameters {

    private static final Map<String, Object> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("snapshot.path", "./snapshots/");
        DEFAULTS.put("snapshot.codeAdmissibleChars", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        DEFAULTS.put("snapshot.uniqueCodeSize", 8);

        DEFAULTS.put("network.PingPongTimeout", 5000);
        DEFAULTS.put("network.directConnectionProbability", 0.7);
        DEFAULTS.put("network.ackTimeout", 5000);

        DEFAULTS.put("logger.loggerName", "DistributedSnapshotLog");
        DEFAULTS.put("logger.path", "./logOutput/");
        DEFAULTS.put("logger.showLogInSOut", false);
        DEFAULTS.put("logger.mute", true);
    }

    public static Object get(String key) {
        return DEFAULTS.get(key);
    }

}
