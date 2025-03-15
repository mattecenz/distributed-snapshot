package polimi.ds.dsnapshot.Snapshot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import polimi.ds.dsnapshot.ApplicationLayerInterface;
import polimi.ds.dsnapshot.Connection.*;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.JavaDSException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;

import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SnapshotTest {

    @Mock
    private ConnectionManager connectionManagerMock;
    @Mock
    private ClientSocketHandler clientSocketHandlerMock;

    private SnapshotManager snapshotManger;


    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        JavaDistributedSnapshot.setConnectionManager(connectionManagerMock);


        snapshotManger = new SnapshotManager(connectionManagerMock);
    }

    @Test
    public void saveApplicationState() throws JavaDSException, InterruptedException {
        ExampleApplicationInterface exampleApplicationInterface = new ExampleApplicationInterface();
        exampleApplicationInterface.state.i=1;

        //set the application interface allowing snapshot to retrieve the application state
        JavaDistributedSnapshot.joinNetwork(exampleApplicationInterface,"friggioggi",0);

        //fake routin table and spanning tree
        RoutingTable mockRoutingTable = new RoutingTable();
        SpanningTree mockSpt = new SpanningTree();
        when(connectionManagerMock.getSpt()).thenReturn(mockSpt);
        when(connectionManagerMock.getRoutingTable()).thenReturn(mockRoutingTable);

        //fake anchor node
        mockSpt.setAnchorNodeHandler(clientSocketHandlerMock);
        when(clientSocketHandlerMock.getRemoteIp()).thenReturn("127.0.0.1");
        when(clientSocketHandlerMock.getRemotePort()).thenReturn(1234);



        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggieri:0"));

        //wait for async method
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                snapshotManger.ManageSnapshotToken("testS1", "friggeri", 0);
            } finally {
                latch.countDown();
            }
        }).start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));

    }


    private static class ExampleApplicationInterface implements ApplicationLayerInterface {
        public ExampleApplicationLayerState state = new ExampleApplicationLayerState();
        @Override
        public <T extends Serializable> void receiveMessage(T messageContent) {

        }

        @Override
        public <T extends Serializable> T getApplicationState() {
            return (T) state;
        }
    }

    private static class ExampleApplicationLayerState implements Serializable {
        public Integer i;
    }

}
