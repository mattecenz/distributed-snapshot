package polimi.ds.dsnapshot.Utilities;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

/**
 * Generic configuration class used to load the config.yaml file of the library.
 */
public class Config {
    /**
     * Map that at each name assigns a particular value.
     */
    private static final Map<String, Object> config;

    // TODO: Idk what the fuck is this. Is this a method? Is it just some code run statically at the start of the application?
    //      in this case the javadoc is not needed. But maybe better to wrap it in a method to match the lifecycle of the library?
    static {
        Map<String, Object> tempConfig = null;
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.yaml")) {
            if (input != null) {
                Yaml yaml = new Yaml();
                tempConfig = yaml.load(input);
            }else {
                LoggerManager.instanceGetLogger().warning("no config.yaml found, using default config parameters");
            }
        } catch (Exception e) {
            LoggerManager.instanceGetLogger().warning("no config.yaml found, using default config parameters");
        }
        config = (tempConfig != null) ? tempConfig : Map.of();
    }

    /**
     * Static method to retrieve the configuration with a key known statically.
     * @param key Name associated with the resource needed.
     * @return The object specified by the key in input. If it was not found, return a default value.
     */
    public static Object get(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> map = config;
        for (int i = 0; i < keys.length - 1; i++) {
            map = (Map<String, Object>) map.get(keys[i]);
            if (map == null) {
                return DefaultConfigParameters.get(key);
            }
        }
        return map.getOrDefault(keys[keys.length - 1], DefaultConfigParameters.get(key));
    }

    /**
     * //TODO: this with a macro would be extremely powerful and clean
     * Static method to retrieve the configuration of a known integer parameter with a key known statically.
     * @param key name associated with the resource needed.
     * @return The integer specified by the key.
     */
    public static int getInt(String key) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
    }
    /**
     * Static method to retrieve the configuration of a known double parameter with a key known statically.
     * @param key name associated with the resource needed.
     * @return The double specified by the key.
     */
    public static Double getDouble(String key) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
    }
    /**
     * Static method to retrieve the configuration of a known string parameter with a key known statically.
     * @param key name associated with the resource needed.
     * @return The string specified by the key.
     */
    public static String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }
    /**
     * Static method to retrieve the configuration of a known boolean parameter with a key known statically.
     * @param key name associated with the resource needed.
     * @return The boolean specified by the key.
     */
    public static boolean getBoolean(String key) {
        Object value = get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
}
