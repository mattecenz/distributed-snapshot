# LIBRARY

## INTERFACE 

When the application is launched, the whole library is treated as a singleton object.

In order to interact with it, we provide several methods which can be called by the frontend of the application in order to create the netwrok and send messages in it.

We reference file [JavaDistributedSnapshot.java](./lib-module/src/main/java/polimi/ds/dsnapshot/Api/JavaDistributedSnapshot.java) for the library, and the file [Main.java](./appExample-module/src/main/java/polimi/ds/dapplication/Main.java) for a possible utilization, but here we leave the main methods with a brief description:

* `startSocketConnection(ip, hostPort, applicationLayerInterface)`: initialize the internal socket of the node and initialize the hook for which the library can communicate with the frontend (explained in detail later).
* `joinNetwork(anchorNodeIp, anchorNodePort)`: connect to a node of an already existing network.
* `leaveNetwork()`: gracefully leave the network, without causing halts.
* `sendMessage(messageContent, destinationIp, destinationPort)`: send a generic message to a node in the network.
* `reconnect()`: after the anchor node of the network has crashed, the user can manually try to reconnect to it to restore the network once the anchor is brought back.
* `startNewSnapshot()`: the node starts a distributed snapshot (unique in the network) and all node save its internal states to disk.
* `restoreSnapshot(snapshotId, snapshotIp, snapshotPort)`: restore a snapshot, provided the unique id, and credentials of the node who created it. It is guaranteed that the network agrees on a single snapshot to restore.
* `getAvailableSnapshots()`: return a string to the user which contains all the information about the snapshots stored on disk.

Moreover, in order to let the library communicate with the frontend, an object [ApplicationLayerInterface](./lib-module/src/main/java/polimi/ds/dsnapshot/Api/ApplicationLayerInterface.java) needs to be created. An example can be found in [AppUtility.java](./appExample-module/src/main/java/polimi/ds/dapplication/AppUtility.java).

The main methods to be implemented are:

* `getApplicationState()`: pass the state of the frontend to the library as a serializable object, which will be saved to disk inside the snapshot.
* `receiveMessage(messageContent)`: the library has received a message for this node, which is forwarded to the frontend as a serializable object and is responsible for decoding it and act accordingly.
* `setApplicationState(appState)`: the library tells the frontend to recover the state once the network has agreed to reconstruct a snapshot.
* `exitNotify(ip, port)`: the library notifies the frontend once a node leaves gracefully the network.
