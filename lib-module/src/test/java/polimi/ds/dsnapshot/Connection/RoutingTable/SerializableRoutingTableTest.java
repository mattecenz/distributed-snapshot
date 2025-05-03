package polimi.ds.dsnapshot.Connection.RoutingTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import polimi.ds.dsnapshot.Connection.ClientSocketHandler;
import polimi.ds.dsnapshot.Connection.NodeName;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.RoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.SerializableRoutingTable;
import polimi.ds.dsnapshot.Connection.SnashotSerializable.RoutingTable.SerializedSocketHandler;
import polimi.ds.dsnapshot.Exception.RoutingTable.RoutingTableNodeAlreadyPresentException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Dictionary;

import static org.mockito.Mockito.when;

public class SerializableRoutingTableTest {

    private RoutingTable routingTable;
    private NodeName nodeName = new NodeName("friggeri",2000);
    @Mock
    ClientSocketHandler clientSocketHandler;
    @Mock
    Socket socket;
    @Mock
    OutputStream out;

    @BeforeEach
    void setUp() throws IOException {
        routingTable = new RoutingTable();

        MockitoAnnotations.openMocks(this);
        when(clientSocketHandler.getRemoteNodeName()).thenReturn(nodeName);
        when(socket.getOutputStream()).thenReturn(out);

        clientSocketHandler = new ClientSocketHandler(socket, nodeName, null, false);
    }

    @Test
    void serializeTest() throws RoutingTableNodeAlreadyPresentException {
        routingTable.addPath(nodeName,clientSocketHandler);

        SerializableRoutingTable serializableRoutingTable = (SerializableRoutingTable)routingTable.toSerialize();

        Dictionary<NodeName, SerializedSocketHandler> routingTableFields = serializableRoutingTable.getOldRoutingTableFields();

        var keys = routingTableFields.keys();
        while(keys.hasMoreElements()) {
            NodeName key = (NodeName) keys.nextElement();
            assert routingTableFields.get(key).getNodeName() == nodeName;
        }
    }

}
