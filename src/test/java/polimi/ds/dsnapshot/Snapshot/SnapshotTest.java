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
import polimi.ds.dsnapshot.Utilities.Config;
import polimi.ds.dsnapshot.Utilities.LoggerManager;
import polimi.ds.dsnapshot.Utilities.SerializationUtils;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SnapshotTest {
    private static final String snapshotPath = Config.getString("snapshot.path");
    @Mock
    private ConnectionManager connectionManagerMock;
    @Mock
    private ClientSocketHandler clientSocketHandlerMock;

    private SnapshotManager snapshotManger;


    @BeforeEach
    public void setup() {
        LoggerManager.start(104);

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
        javaDistributedSnapshot.setApplicationLayerInterface(exampleApplicationInterface);
        javaDistributedSnapshot.joinNetwork("friggioggi",0);

        //fake routing table and spanning tree
        RoutingTable mockRoutingTable = new RoutingTable();
        SpanningTree mockSpt = new SpanningTree();
        when(connectionManagerMock.getSpt()).thenReturn(mockSpt);
        when(connectionManagerMock.getRoutingTable()).thenReturn(mockRoutingTable);

        //fake anchor node
        when(clientSocketHandlerMock.getRemoteNodeName()).thenReturn(new NodeName("127.0.0.1",1234));
        mockSpt.setAnchorNodeHandler(clientSocketHandlerMock);

        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggieri:0"));

        //wait for async method
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                snapshotManger.manageSnapshotToken("testS1", new NodeName("friggieri",0));
            } finally {
                latch.countDown();
            }
        }).start();
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        Thread.sleep(1000);//to be sure the file is saved

        Serializable applicationState = snapshotManger.getLastSnapshot(0).getApplicationState();

        assertDoesNotThrow(() -> {
            ExampleApplicationLayerState savedApplicationState = (ExampleApplicationLayerState) applicationState;
            System.out.println(savedApplicationState.i);
            assertEquals(savedApplicationState.i, exampleApplicationInterface.state.i);
        });
    }

    @Test
    public void testGetSnapshotNull() throws IOException {
        this.deleteAllFiles(snapshotPath);
        SnapshotState snapshotState = snapshotManger.getLastSnapshot(1);
        assert (snapshotState==null);
    }

    @Test
    public void SnapshotWIthMessagesTest() throws JavaDSException, EventException, InterruptedException {
        //input messages
        List<String> snapshotStarterMessages = Arrays.asList("1","T","2","3");
        List<String> n1Messages = Arrays.asList("6","7","8","9","T","10","11");
        List<String> n2Messages = Arrays.asList("T","4","5");

        //expected snapshot content
        List<String> snapshotMessagesContent = Arrays.asList("6","7","8","9");

        this.testTokenSupport(snapshotStarterMessages,n1Messages,n2Messages,snapshotMessagesContent,2);
    }


    @Test
    public void SnapshotWIthMessagesTest2() throws JavaDSException, EventException, InterruptedException {
        //input messages
        List<String> snapshotStarterMessages = Arrays.asList("1","T","2","3");
        List<String> n1Messages = Arrays.asList("T","10","11");
        List<String> n2Messages = Arrays.asList("T","4","5");

        //expected snapshot content
        List<String> snapshotMessagesContent = Arrays.asList();

        this.testTokenSupport(snapshotStarterMessages,n1Messages,n2Messages,snapshotMessagesContent,3);
    }

    @Test
    public void SnapshotWIthMessagesTest3() throws JavaDSException, EventException, InterruptedException {
        //input messages
        List<String> snapshotStarterMessages = Arrays.asList("1","T","2","3");
        List<String> n1Messages = Arrays.asList("6","7","8","9","T","10","11");
        List<String> n2Messages = Arrays.asList("12","13","14","T","4","5");

        //expected snapshot content
        List<String> snapshotMessagesContent = Arrays.asList("6","7","8","9","12","13","14");

        this.testTokenSupport(snapshotStarterMessages,n1Messages,n2Messages,snapshotMessagesContent,4);
    }


    private void testTokenSupport(List<String> snapshotStarterMessages, List<String> n1Messages,List<String> n2Messages,List<String> snapshotMessagesContent, int port) throws InterruptedException, JavaDSException, EventException {
        //application state
        ExampleApplicationInterface exampleApplicationInterface = new ExampleApplicationInterface();
        Random rand = new Random();
        exampleApplicationInterface.state.i = rand.nextInt();

        //set the application interface allowing snapshot to retrieve the application state
        JavaDistributedSnapshot javaDistributedSnapshot =  JavaDistributedSnapshot.getInstance();
        javaDistributedSnapshot.setApplicationLayerInterface(exampleApplicationInterface);
        javaDistributedSnapshot.joinNetwork("friggioggi",0);

        //fake routing table and spanning tree
        RoutingTable mockRoutingTable = new RoutingTable();
        SpanningTree mockSpt = new SpanningTree();
        when(connectionManagerMock.getSpt()).thenReturn(mockSpt);
        when(connectionManagerMock.getRoutingTable()).thenReturn(mockRoutingTable);

        //fake anchor node
        when(clientSocketHandlerMock.getRemoteNodeName()).thenReturn(new NodeName("127.0.0.1",1234));
        mockSpt.setAnchorNodeHandler(clientSocketHandlerMock);

        //messages
        Stack<String> snapshotStarterStack= new Stack<>();
        snapshotStarterStack.addAll(snapshotStarterMessages.reversed());

        Stack<String> n1Stack= new Stack<>();
        n1Stack.addAll(n1Messages.reversed());
        Stack<String> n2Stack= new Stack<>();
        n2Stack.addAll(n2Messages.reversed());

        //event
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggieri:"+port)); //snapshotStarter
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggioggi:"+port)); //n1
        assertDoesNotThrow(() -> EventsBroker.createEventChannel("friggidomani:"+port)); //n2

        socketEmulator("friggieri",port,snapshotStarterStack.pop());
        socketEmulator("friggieri",port,snapshotStarterStack.pop()); //get first token

        Thread snapshotStarterThread = new Thread(() -> {
            while (!snapshotStarterStack.empty()){
                assertDoesNotThrow(() -> socketEmulator("friggieri",port,snapshotStarterStack.pop()));
            }
        });
        snapshotStarterThread.start();

        Thread n1Thread = new Thread(() -> {
            while (!n1Stack.empty()){
                assertDoesNotThrow(() -> socketEmulator("friggioggi",port,n1Stack.pop()));
            }
        });
        n1Thread.start();

        Thread n2Thread = new Thread(() -> {
            while (!n2Stack.empty()){
                assertDoesNotThrow(() -> socketEmulator("friggidomani",port,n2Stack.pop()));
            }
        });
        n2Thread.start();
        snapshotStarterThread.join();
        n1Thread.join();
        n2Thread.join();

        Thread.sleep(1000);//to be sure the file is saved

        SnapshotState savedSnapshotState = snapshotManger.getLastSnapshot(port);
        assertNotNull(savedSnapshotState);
        Serializable applicationState = savedSnapshotState.getApplicationState();

        assertDoesNotThrow(() -> {
            ExampleApplicationLayerState savedApplicationState = (ExampleApplicationLayerState) applicationState;
            System.out.println("saved snapshot state: " + savedApplicationState.i);
            assertEquals(savedApplicationState.i, exampleApplicationInterface.state.i);
        });

        System.out.println("messages saved number: " + savedSnapshotState.getMessageInputStack().size());
        assert(savedSnapshotState.getMessageInputStack().size()==snapshotMessagesContent.size());
        for(CallbackContentWithName c : savedSnapshotState.getMessageInputStack()){
            ApplicationMessage am = (ApplicationMessage) c.getCallBackMessage();
            String s = (String)am.getApplicationContent();
            System.out.println("saved message content: " + s);
            assertDoesNotThrow(() -> {assert(snapshotMessagesContent.contains(s));});
        }
    }

    private void socketEmulator(String ip, int port, String message) throws EventException {
        if(Objects.equals(message, "T")){
            System.out.println("token received from channel: " + ip + ":" + port);
            snapshotManger.manageSnapshotToken("testS2", new NodeName(ip,port));
        }else {
            EventsBroker.getEventChannel(ip+":"+port).publish(new ApplicationMessage(message,new NodeName("test",1234),new NodeName(ip,port)));
        }
    }
    private static class ExampleApplicationInterface implements ApplicationLayerInterface {
        public ExampleApplicationLayerState state = new ExampleApplicationLayerState();
        @Override
        public void receiveMessage(Serializable messageContent) {

        }

        @Override
        public <T extends Serializable> T getApplicationState() {
            return (T) state;
        }

        @Override
        public void exitNotify(String ip, int port) {
            //do nothing
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
