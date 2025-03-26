package polimi.ds.dsnapshot.Utilities;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class Config {
    private static final Map<String, Object> config;

    static {
        Map<String, Object> tempConfig = null;
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.yaml")) {
            if (input != null) {
                Yaml yaml = new Yaml();
                tempConfig = yaml.load(input);
            }else {
                System.out.println("no config.yaml found, using default config parameters");
            }
        } catch (Exception e) {
            System.out.println("no config.yaml found, using default config parameters");
        }
        config = (tempConfig != null) ? tempConfig : Map.of();
    }

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

    public static int getInt(String key) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
    }

    public static Double getDouble(String key) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
    }

    public static String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

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
