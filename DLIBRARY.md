# COMPONENTS

The main components of the library are:

## INTERFACE 

From which the application will interact with internal. In particular
these methods are:

* **setup()**
* **sendMsg(msg,to)**
* **startSnapshot()**
* **connectToNetwork(ip_known_node)**
* **restoreState()** <- returns the state of the client or passes it as an argument
* **exit()**

These are only the methods that the client can directly invoke. 
It also needs to expose some methods that notify the client that 
a message has arrived and he needs to process it (event listener?).

## INTERNAL STATE

This is basically everything that is saved in the snapshot to restore
the execution correctly.

There is:

* **routing table** : \<ip:port,next_hop\>
* **connections active** : List\<socket_descriptors\>
* **anchor node** : socket_descriptor
* **internal message scheduler** : List\<messages\>
* **pointer to state of the client**

Then it will need also to expose some sort of "state class" to save the 
state of the client.

# PSEUDOEXCEUTION

NB: snapshot is easy, do not put it in here yet

At the **start**:

The client who uses the library needs to call a **setup function** to
create all the objects that it needs.
Here we should also pass as argument to the function the **ip** of the node
that we want to establish a direct connection and a ping with.

After that from the client side the library is basically *passive* and 
he contacts it only when he needs to do something.

While internally the library:

* connects to the peer specified by the client: sends a **askJoin()** msg
and waits for a response in order to start the **pingPong**. If no response
just tell the client to connect to another ip.
* then starts listening for incoming connections and waits to see if someone
wants to establish a direct connection.

NB: *I think that when a new node joins the network it is best if the message is* **broadcasted**

Now the library acts in different ways depending on the incoming messages:

* **askJoin()** ack and establish direct connection. If node unreachable then do nothing
(node not connected to network still). If reachable then communicate to neighbours
(nodes in routing table with direct connection + anchor) that a new node joined.
* **newNode()** add to routing table, and with a probability create a direct connection by
sending a specific message **wantToCreateConnection()**
* **wantToCreateConnection()** ack and add to routing table (no ping started)
* **ping()** just **pong()**. If unreachable initiate procedure for failure detection
* **messageForHim()** consume it
* **messageNotForHim()** look in routing table, if none present then ?
(either activate discovery procedure or exception).
* **broadcast()** consume (ack) back and send to all other channels. Decide if with probability
create a new direct connection.
* **exit()** a node with which i was ponging exits the network, act as above.

In the case of the failure of the node (crash, **NB:** understand what to do with failures of networks):

* if the node crashed is **itself**, not much you can do.

Mostly it will happen due to pings. In this case we need to differentiate what
the processor sent:

* **ping()**: pong crashed, need to try to attach again to a node (remove from routing table). 
If none present then become orphan and let the client handle it.
* **pong()**: ping crashed, just notify all remaining processes that node crashed
and remove from routing table.

In all the other cases not much can be done, if the message could not be sent
because the peer crashed then return to the original client an exception.

Anyway **the only objective** of the library is to try and keep the network reachable.
If the message cannot be sent then the application must be aware of it and restart the application
from a snapshot if needed.







