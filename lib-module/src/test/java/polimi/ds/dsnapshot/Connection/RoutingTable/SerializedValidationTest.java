package polimi.ds.dsnapshot.Connection.RoutingTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.RoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.SerializableRoutingTable;
import polimi.ds.dsnapshot.Exception.RoutingTable.RoutingTableNodeAlreadyPresentException;
import polimi.ds.dsnapshot.Exception.RoutingTable.RoutingTableNodeNotPresentException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

public class SerializedValidationTest {
        private RoutingTable routingTable;
        private SerializableRoutingTable serializableRoutingTable;

        @Mock
        Socket socket;
        @Mock
        OutputStream out;

        @BeforeEach
        void setUp() throws IOException {
            MockitoAnnotations.openMocks(this);
            when(socket.getOutputStream()).thenReturn(out);

            routingTable = new RoutingTable();
        }

        @Test
        void validationSuccessTest() throws RoutingTableNodeAlreadyPresentException {
            this.setRoutingTableFields(3,0);
            serializableRoutingTable = (SerializableRoutingTable)routingTable.toSerialize();

            assert serializableRoutingTable != null;
            assert routingTable.serializedValidation(serializableRoutingTable);
        }

        @Test
        void validationFailureTest1() throws RoutingTableNodeAlreadyPresentException {
            this.setRoutingTableFields(3,1);
            serializableRoutingTable = (SerializableRoutingTable)routingTable.toSerialize();

            NodeName problematicNode = new NodeName("nodeP", 10);
            ClientSocketHandler problematicSocket = new ClientSocketHandler(socket, problematicNode);
            routingTable.addPath(problematicNode, problematicSocket);

            assert serializableRoutingTable != null;
            assert !routingTable.serializedValidation(serializableRoutingTable);
        }

    @Test
    void validationFailureTest2() throws RoutingTableNodeAlreadyPresentException, RoutingTableNodeNotPresentException {
        this.setRoutingTableFields(3,2);
        serializableRoutingTable = (SerializableRoutingTable) routingTable.toSerialize();

        NodeName problematicNode = new NodeName("Node00012", 10);
        routingTable.removePath(problematicNode);

        assert serializableRoutingTable != null;
        assert !routingTable.serializedValidation(serializableRoutingTable);
    }

    private void setRoutingTableFields(int nodeNum, int testNum) throws RoutingTableNodeAlreadyPresentException {
        routingTable.clearTable();

        List<NodeName> nodes = new ArrayList<>();
        List<ClientSocketHandler> handlers = new ArrayList<>();

        for (int i = 0; i < nodeNum; i++) {
            nodes.add(new NodeName("Node000"+i + testNum,10));
            handlers.add(new ClientSocketHandler(socket, nodes.get(i), null));
            routingTable.addPath(nodes.get(i),handlers.get(i));
        }
    }
}
