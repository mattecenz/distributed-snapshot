package polimi.ds.dsnapshot.Connection;

public class RoutingTableTest {

    private RoutingTable routingTable;
    private ClientSocketHandler socketHandler;


    /*@BeforeEach
    void setUp() {
        // Inizializza una nuova tabella di routing prima di ogni test
        routingTable = new RoutingTable();

        socketHandler = new ClientSocketHandler(new Socket());
    }

    @Test
    void testAddPathSuccessfully() throws RoutingTableException {
        NetNode node1 = new NetNode("Node1",10);

        routingTable.addPath(node1, socketHandler);

        assertNotNull(routingTable.getNextHop(node1));
        assertEquals(socketHandler, routingTable.getNextHop(node1));
    }

    @Test
    void testAddPathThrowsExceptionForDuplicateNode() {
        NetNode node1 = new NetNode("Node2",10);

        assertDoesNotThrow(() -> routingTable.addPath(node1, socketHandler));

        RoutingTableException exception = assertThrows(RoutingTableException.class, () -> {
            routingTable.addPath(node1, socketHandler);
        });

        assertEquals("destination already in the table", exception.getMessage());
    }

    @Test
    void testUpdatePathThrowsExceptionForNonExistentNode() {
        NetNode node1 = new NetNode("Node4",10);
        RoutingTableException exception = assertThrows(RoutingTableException.class, () -> {
            routingTable.updatePath(node1, socketHandler);
        });

        assertEquals("destination isn't present the table", exception.getMessage());
    }

    @Test
    void testClearRoutingTable() throws RoutingTableException {
        NetNode node1 = new NetNode("Node5",10);
        NetNode node2 = new NetNode("Node6",10);

        routingTable.addPath(node1, socketHandler);
        routingTable.addPath(node2, socketHandler);

        routingTable.clearTable();

        assertTrue(routingTable.isEmpty());
    }

    @Test
    void testGetPathSuccessfully() throws RoutingTableException {
        NetNode node1 = new NetNode("Node7",10);

        routingTable.addPath(node1, socketHandler);

        ClientSocketHandler retrievedHandler = routingTable.getNextHop(node1);

        assertNotNull(retrievedHandler);
        assertEquals(socketHandler, retrievedHandler);
    }



    @Test
    void testGetPathThrowsExceptionForNonExistentNode() {
        NetNode node1 = new NetNode("Node8",10);

        RoutingTableException exception = assertThrows(RoutingTableException.class, () -> {
            routingTable.getNextHop(node1);
        });

        assertEquals("destination isn't present the table", exception.getMessage());
    }

    @Test
    void testRemoveAllIndirectPathSuccessfully() throws RoutingTableException {
        NetNode node = new NetNode("Node9",10);
        NetNode node1 = new NetNode("Node10",10);

        routingTable.addPath(node, socketHandler);
        routingTable.addPath(node1, socketHandler);

        routingTable.removeAllIndirectPath(socketHandler);

        assertTrue(routingTable.isEmpty());
    }

    @Test
    void testRemoveAllIndirectPathSuccessfully1() throws RoutingTableException {
        NetNode node = new NetNode("Node9",10);
        NetNode node1 = new NetNode("Node10",10);

        ClientSocketHandler socketHandler1 = new ClientSocketHandler(new Socket());;

        routingTable.addPath(node, socketHandler);
        routingTable.addPath(node1, socketHandler1);

        routingTable.removeAllIndirectPath(socketHandler);

        assertFalse(routingTable.isEmpty());
    }*/
}