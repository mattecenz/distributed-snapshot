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
import polimi.ds.dsnapshot.Connection.Messages.ApplicationMessage;
import polimi.ds.dsnapshot.Connection.Messages.Message;
import polimi.ds.dsnapshot.Events.CallbackContent.CallbackContentWithName;
import polimi.ds.dsnapshot.Events.EventsBroker;
import polimi.ds.dsnapshot.Exception.EventException;
import polimi.ds.dsnapshot.Exception.JavaDSException;
import polimi.ds.dsnapshot.JavaDistributedSnapshot;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        JavaDistributedSnapshot javaDistributedSnapshot =  JavaDistributedSnapshot.getInstance();
        javaDistributedSnapshot.setConnectionManager(connectionManagerMock);

        snapshotManger = new SnapshotManager(connectionManagerMock);
        System.out.println(" ");

        EventsBroker.removeAllEventsChannel();
    }

    @Test
    public void SnapshotNoMessagesTest() throws JavaDSException, InterruptedException {
        ExampleApplicationInterface exampleApplicationInterface = new ExampleApplicationInterface();
        Random rand = new Random();
        exampleApplicationInterface.state.i = rand.nextInt();

        //set the application interface allowing snapshot to retrieve the application state
        JavaDistributedSnapshot javaDistributedSnapshot =  JavaDistributedSnapshot.getInstance();
        javaDistributedSnapshot.joinNetwork(exampleApplicationInterface,"friggioggi",0);

        //fake routing table and spanning tree
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
                snapshotManger.manageSnapshotToken("testS1", "friggieri", 0);
            } finally {
                latch.countDown();
            }
        }).start();
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        Thread.sleep(1000);//to be sure the file is saved

        byte[] applicationState = snapshotManger.getLastSnapshot().getApplicationState();

        assertDoesNotThrow(() -> {
            ExampleApplicationLayerState savedApplicationState = SerializationUtils.deserialize(applicationState);
            System.out.println(savedApplicationState.i);
            assertEquals(savedApplicationState.i, exampleApplicationInterface.state.i);
        });
    }

    @Test
    public void testGetSnapshotNull() throws IOException {
        this.deleteAllFiles(snapshotPath);
        SnapshotState snapshotState = snapshotManger.getLastSnapshot();
        assert (snapshotState==null);
    }

    @Test
    public void SnapshotWIthMessagesTest() throws JavaDSException, EventException, InterruptedException {
        //application state
        ExampleApplicationInterface exampleApplicationInterface = new ExampleApplicationInterface();
        Random rand = new Random();
        exampleApplicationInterface.state.i = rand.nextInt();

        //set the application interface allowing snapshot to retrieve the application state
        JavaDistributedSnapshot javaDistributedSnapshot =  JavaDistributedSnapshot.getInstance();
        javaDistributedSnapshot.joinNetwork(exampleApplicationInterface,"friggioggi",0);

        //fake routing table and spanning tree
        RoutingTable mockRoutingTable = new RoutingTable();
        SpanningTree mockSpt = new SpanningTree();
        when(connectionManagerMock.getSpt()).thenReturn(mockSpt);
        when(connectionManagerMock.getRoutingTable()).thenReturn(mockRoutingTable);

        //fake anchor node
        mockSpt.setAnchorNodeHandler(clientSocketHandlerMock);
        when(clientSocketHandlerMock.getRemoteIp()).thenReturn("127.0.0.1");
        when(clientSocketHandlerMock.getRemotePort()).thenReturn(1234);

        //messages
        List<String> snapshotStarterMessages = Arrays.asList("1","T","2","3");
        Stack<String> snapshotStarterStack= new Stack<>();
        snapshotStarterStack.addAll(snapshotStarterMessages.reversed());
        List<String> n1Messages = Arrays.asList("6","7","8","9","T","10","11");
        Stack<String> n1Stack= new Stack<>();
        n1Stack.addAll(n1Messages.reversed());
        List<String> n2Messages = Arrays.asList("T","4","5");
        Stack<String> n2Stack= new Stack<>();
        n2Stack.addAll(n2Messages.reversed());

        //event
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggieri:0")); //snapshotStarter
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggioggi:0")); //n1
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggidomani:0")); //n2

        socketEmulator("friggieri",0,snapshotStarterStack.pop());
        socketEmulator("friggieri",0,snapshotStarterStack.pop()); //get first token

        Thread snapshotStarterThread = new Thread(() -> {
            while (!snapshotStarterStack.empty()){
                assertDoesNotThrow(() -> socketEmulator("friggieri",0,snapshotStarterStack.pop()));
            }
        });
        snapshotStarterThread.start();

        Thread n1Thread = new Thread(() -> {
            while (!n1Stack.empty()){
                assertDoesNotThrow(() -> socketEmulator("friggioggi",0,n1Stack.pop()));
            }
        });
        n1Thread.start();

        Thread n2Thread = new Thread(() -> {
            while (!n2Stack.empty()){
                assertDoesNotThrow(() -> socketEmulator("friggidomani",0,n2Stack.pop()));
            }
        });
        n2Thread.start();
        snapshotStarterThread.join();
        n1Thread.join();
        n2Thread.join();

        Thread.sleep(1000);//to be sure the file is saved

        SnapshotState savedSnapshotState = snapshotManger.getLastSnapshot();
        assertNotNull(savedSnapshotState);
        byte[] applicationState = savedSnapshotState.getApplicationState();

        assertDoesNotThrow(() -> {
            ExampleApplicationLayerState savedApplicationState = SerializationUtils.deserialize(applicationState);
            System.out.println(savedApplicationState.i);
            assertEquals(savedApplicationState.i, exampleApplicationInterface.state.i);
        });

        List<String> snapshotMessagesContent = Arrays.asList("6","7","8","9");
        assert(savedSnapshotState.getMessageInputStack().size()==snapshotMessagesContent.size());
        for(CallbackContentWithName c : savedSnapshotState.getMessageInputStack()){
            System.out.println(c.getEventName());
            ApplicationMessage am = (ApplicationMessage) c.getCallBackMessage();
            String s = new String(am.getApplicationContent());
            assertDoesNotThrow(() -> {assert(snapshotMessagesContent.contains(s));});
        }
    }


    private void socketEmulator(String ip, int port, String message) throws EventException {
        if(Objects.equals(message, "T")){
            System.out.println("token received");
            snapshotManger.manageSnapshotToken("testS2", ip, port);
        }else {
            byte [] messageBytes = message.getBytes();
            EventsBroker.getEventChannel(ip+":"+port).publish(new ApplicationMessage(messageBytes,ip,port,false));
        }
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
