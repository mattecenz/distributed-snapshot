package polimi.ds.dsnapshot.Connection.RoutingTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.RoutingTable;
import polimi.ds.dsnapshot.Exception.RoutingTableNodeAlreadyPresentException;
import polimi.ds.dsnapshot.Exception.RoutingTableNodeNotPresentException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class RoutingTableTest {

    private RoutingTable routingTable;
    private ClientSocketHandler socketHandler;

    @Mock
    Socket socket;
    @Mock
    OutputStream out;


    @BeforeEach
    void setUp() throws IOException {
        // Inizializza una nuova tabella di routing prima di ogni test
        routingTable = new RoutingTable();

        MockitoAnnotations.openMocks(this);

        // Mock del comportamento di getOutputStream()
        when(socket.getOutputStream()).thenReturn(out);
    }

    @Test
    void testAddPathSuccessfully() throws RoutingTableNodeAlreadyPresentException, RoutingTableNodeNotPresentException {
        NodeName node1 = new NodeName("Node1",10);
        socketHandler = new ClientSocketHandler(socket, node1, null);

        routingTable.addPath(node1, socketHandler);

        NodeName nodeTest = new NodeName("Node1",10);
        assertDoesNotThrow(() ->{
            routingTable.getNextHop(nodeTest);
        });
        assertEquals(socketHandler, routingTable.getNextHop(nodeTest));
    }

    @Test
    void testAddPathThrowsExceptionForDuplicateNode() {
        NodeName node1 = new NodeName("Node2",10);
        socketHandler = new ClientSocketHandler(socket, node1, null);

        assertDoesNotThrow(() -> routingTable.addPath(node1, socketHandler));

        assertThrows(RoutingTableNodeAlreadyPresentException.class, () -> {
            routingTable.addPath(node1, socketHandler);
        });
    }

    @Test
    void testUpdatePathThrowsExceptionForNonExistentNode() {
        NodeName node1 = new NodeName("Node4",10);
        socketHandler = new ClientSocketHandler(socket, node1, null);

        assertThrows(RoutingTableNodeNotPresentException.class, () -> {
            routingTable.updatePath(node1, socketHandler);
        });
    }

    @Test
    void testClearRoutingTable() throws RoutingTableNodeAlreadyPresentException {
        NodeName node1 = new NodeName("Node5",10);
        NodeName node2 = new NodeName("Node6",10);
        socketHandler = new ClientSocketHandler(socket, node1, null);

        routingTable.addPath(node1, socketHandler);
        routingTable.addPath(node2, socketHandler);

        routingTable.clearTable();

        assertTrue(routingTable.isEmpty());
    }

    @Test
    void testGetPathSuccessfully() throws RoutingTableNodeAlreadyPresentException, RoutingTableNodeNotPresentException {
        NodeName node1 = new NodeName("Node7",10);
        socketHandler = new ClientSocketHandler(socket, node1, null);

        routingTable.addPath(node1, socketHandler);

        ClientSocketHandler retrievedHandler = routingTable.getNextHop(node1);

        assertNotNull(retrievedHandler);
        assertEquals(socketHandler, retrievedHandler);
    }



    @Test
    void testGetPathThrowsExceptionForNonExistentNode() {
        NodeName node1 = new NodeName("Node8",10);
        socketHandler = new ClientSocketHandler(socket, node1, null);

        assertThrows(RoutingTableNodeNotPresentException.class, () -> {
            routingTable.getNextHop(node1);
        });
    }

    @Test
    void testRemoveAllIndirectPathSuccessfully() throws RoutingTableNodeAlreadyPresentException {
        NodeName node = new NodeName("Node9",10);
        NodeName node1 = new NodeName("Node10",10);
        socketHandler = new ClientSocketHandler(socket, node1, null);

        routingTable.addPath(node, socketHandler);
        routingTable.addPath(node1, socketHandler);

        routingTable.removeAllIndirectPath(socketHandler);

        assertTrue(routingTable.isEmpty());
    }


}