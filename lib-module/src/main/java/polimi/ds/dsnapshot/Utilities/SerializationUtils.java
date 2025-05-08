package polimi.ds.dsnapshot.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

/** TODO: they are kinda not needed anymore, can be removed. I will avoid commenting them.
 * Class wrapping some utils for serialization.
 */
public class SerializationUtils {
    public static <T extends Serializable> byte[] serialize(T object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
             oos.writeObject(object);
             return bos.toByteArray();
        }
    }
    @SuppressWarnings("unchecked") //Unchecked cast: 'java. lang. Object' to 'T'
    public static <T extends Serializable> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
             return (T) ois.readObject();
        }
    }

}
