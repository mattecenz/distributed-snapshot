package polimi.ds.dsnapshot.Events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Utilities.LoggerManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class EventsBrokerTest {

    @BeforeEach
    public void setup() {
        LoggerManager.start(104);
        EventsBroker.removeAllEventsChannel();
    }

    @Test
    public void createEvent() {
        Event e1 = null, e2 = null;
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("e1"));
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("e2"));

        assertDoesNotThrow(() ->EventsBroker.getEventChannel("e1"));
        assertDoesNotThrow(() ->EventsBroker.getEventChannel("e2"));

        try {
            e1 = EventsBroker.getEventChannel("e1");
            e2 = EventsBroker.getEventChannel("e2");
        }catch (Exception e) {
            e.printStackTrace();
        }

        assert e1 != null;
        assert e2 != null;
        assert !e1.equals(e2);
    }

    @Test
    public void testException(){
        boolean exceptionThrown = false;

        assertDoesNotThrow(() -> EventsBroker.createEventChannel("e1"));
        try {
            EventsBroker.createEventChannel("e1");
        } catch (EventException e) {
            exceptionThrown = true;
        }

        assert exceptionThrown;
        exceptionThrown = false;

        try {
            EventsBroker.getEventChannel("e2");
        } catch (EventException e) {
            exceptionThrown = true;
        }
        assert exceptionThrown;
    }
}
