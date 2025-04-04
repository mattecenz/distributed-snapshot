package polimi.ds.dsnapshot.Utilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

public class SerializeTest {

    @BeforeEach
    public void setup() {
        LoggerManager.start(104);
    }

    @Test
    public void serializationTest() throws IOException {
        ExampleMessage message = new ExampleMessage("frigieri, frigioggi, frigdomani");

        byte[] serialized = SerializationUtils.serialize(message);


        ExampleMessage deserializedMessage = null;
        try {
            deserializedMessage = SerializationUtils.deserialize(serialized);
        } catch (ClassNotFoundException e) {
            assert false;
        }

        assert deserializedMessage != null;

        assert deserializedMessage.getMessage().equals(message.getMessage());
        System.out.println(deserializedMessage.getMessage());
    }

    @Test
    public void wtfTest(){
        boolean exceptionThrown = false;

        byte[] serialized = new byte[10];
        System.out.println(Arrays.toString(serialized));
        try {
            SerializationUtils.deserialize(serialized);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage() + " " + e.getClass());
            exceptionThrown = true;
        }

        assert exceptionThrown;
    }

    @Test
    public void wtfStringTest(){
        boolean exceptionThrown = false;
        String s = "testString";
        String deserializedString = null;

        byte[] serialized = s.getBytes();
        System.out.println(Arrays.toString(serialized));
        try {
            SerializationUtils.deserialize(serialized);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage() + " " + e.getClass());
            exceptionThrown = true;
        }

        assert exceptionThrown;

        try {
            serialized = SerializationUtils.serialize(s);
            deserializedString = SerializationUtils.deserialize(serialized);
        } catch (IOException | ClassNotFoundException e) {
            assert false;
        }

        assert deserializedString != null;
        assert deserializedString.equals(s);
    }






    private static class ExampleMessage implements Serializable {

        String message;
        public ExampleMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
