package polimi.ds.dsnapshot.Snapshot;

import static org.junit.jupiter.api.Assertions.*;
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
import polimi.ds.dsnapshot.Utilities.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class SnapshotTest {
    private static final String snapshotPath = "./snapshots/"; //todo config param
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
        System.out.println(" ");
    }

    @Test
    public void saveApplicationStateTest() throws JavaDSException, InterruptedException {
        ExampleApplicationInterface exampleApplicationInterface = new ExampleApplicationInterface();
        Random rand = new Random();
        exampleApplicationInterface.state.i = rand.nextInt();  // This will give a random integer

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

        Thread.sleep(1000);//to be sure the file is saved

        byte[] applicationState = SnapshotManager.getLastSnapshot().getApplicationState();

        assertDoesNotThrow(() -> {
            ExampleApplicationLayerState savedApplicationState = SerializationUtils.deserialize(applicationState);
            System.out.println(savedApplicationState.i);
            assertEquals(savedApplicationState.i, exampleApplicationInterface.state.i);
        });
    }



    @Test
    public void testGetSnapshotNull() throws IOException {
        System.out.println("this test is going to delete all snapshots \n want to procede [Y\n]:");
        this.deleteAllFiles(snapshotPath);
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

    private void deleteAllFiles(String directoryPath) throws IOException {
        Path dirPath = Paths.get(directoryPath);

        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            System.out.println("Directory does not exist: " + directoryPath);
        }
    }

}
