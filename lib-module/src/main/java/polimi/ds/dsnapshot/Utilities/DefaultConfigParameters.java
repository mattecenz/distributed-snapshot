package polimi.ds.dsnapshot.Utilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Class containing all the default configuration parameters of the library.
 */
public class DefaultConfigParameters {
    /**
     * Static map which to each key associates a default value.
     */
    private static final Map<String, Object> DEFAULTS = new HashMap<>();
    // TODO: idk again this stuff.
    static {
        DEFAULTS.put("snapshot.path", "./snapshots/");
        DEFAULTS.put("snapshot.codeAdmissibleChars", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        DEFAULTS.put("snapshot.uniqueCodeSize", 8);
        DEFAULTS.put("snapshot.snapshotRestore2PCTimeout", 10000);

        DEFAULTS.put("network.PingPongTimeout", 5000);
        DEFAULTS.put("network.directConnectionProbability", 0.7);
        DEFAULTS.put("network.ackTimeout", 5000);

        DEFAULTS.put("logger.loggerName", "DistributedSnapshotLog");
        DEFAULTS.put("logger.path", "./logOutput/");
        DEFAULTS.put("logger.showLogInSOut", false);
        DEFAULTS.put("logger.mute", true);
    }

    /**
     * Generic getter of a default value given an input key.
     * @param key Input key to search.
     * @return The value associated with the input key.
     */
    public static Object get(String key) {
        return DEFAULTS.get(key);
    }

}
