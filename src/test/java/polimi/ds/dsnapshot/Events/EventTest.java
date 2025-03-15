package polimi.ds.dsnapshot.Events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Connection.Messages.MessageAck;

import java.util.function.Consumer;

public class EventTest {

    private Event e1,e2;

    private Message m1,m2;

    private Consumer<Message> c1,c2;

    @BeforeEach
    void setUp() {
        m1 = null;
        m2 = null;

        e1 = new Event("testE1");
        c1 = this::e1Callback;
        e1.subscribe(c1);
        e2 = new Event("testE2");
        c2 = this::e2Callback;
        e2.subscribe(c2);
    }

    private void e1Callback(Message m) {
        this.m1 = m;
    }

    private void e2Callback(Message m) {
        this.m2 = m;
    }

    @Test
    void singleEventTest() {
        Message tmp = new MessageAck(0);
        e1.publish(tmp);

        assert m1!=null;
        assert m2==null;
        assert m1.equals(tmp);
    }

    @Test
    void multipleCallbackTest() {
        Message tmp = new MessageAck(0);
        Message tmp2 = new MessageAck(0);
        e1.publish(tmp);
        e2.publish(tmp2);

        assert m1!=null;
        assert m2!=null;
        assert !m1.equals(m2);
    }
    @Test
    void unsubscribeTest() {
        Message tmp = new MessageAck(0);
        Message tmp2 = new MessageAck(0);
        Message tmp3 = new MessageAck(0);
        e1.publish(tmp);
        e1.unsubscribe(c1);
        e1.publish(tmp2);

        assert m1!=null;
        assert m2==null;
        assert m1.equals(tmp);

        m1 = null;

        e1.subscribe(c1);
        e1.publish(tmp3);

        assert m1!=null;
        assert m2==null;
        assert m1.equals(tmp3);
    }

}
