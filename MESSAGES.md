# MEESSAGES

Try to understand how we need to act when the system is operating in normal, join/leave, snapshot mode.

**NB** *This could be represented as a state machine.*

## NORMAL

While operating in normal mode two peers need to exchange only two messages:

* **heartbeat** (asynchronously every X amount of time) with ack
* **messages to update the state of the application** (can be direct between the peers or it can be forwarded to another one). The ack is decided by the application if it is needed or not(can also use nacks, but need to change assumptions and a bit more tricky).

## JOIN

When a peer joins the network we need to build his own routing table and update the ones of his "neighbours".

The first assumption we need is: *when a node wants to join the network it needs to know at least one name of a peer already in the network.*

In this way we can send a **askJoin** messsage to it and establish a connection by sending back an ack. 

But the distributed snapshot works well with a dense network, so the peer needs to establish more connections.

A solution could be to build a spanning tree of the network. 
But if we assume *that the node already present in the network has his own routing table complete* then the protocol is easy to implement:

* node A asks B to join the network
* node B has his routing table already full so he sends to everyone a message that node A wants to join the network
* every node pings with A and starts communicating messages if needed.

This works but it has two main issues to solve:

* what if node B has its routing table not updated? An easy example is that node B is waiting for the ping of another node C which just joined the network. Then a direct connection between node A and C can potentially not be established. But it has an easy solution with **atomic operations**.
* the network becomes pretty dense ( O(n*e) ), so the number of direct connections (and pings) becomes big very quickly.

The second problem is not that trivial to solve, but the main idea still remains there: we need to build a spanning tree of the network.

We could use a hierarchical structure, and decide manually which are the "supernodes" that can communicate by flooding.

Or maybe a better solution is to use a probabilistic approach. So **node C establishes a direct connection with node A with X% probability**. Of course node B will have a 100% probability to connect.

But we still need to guarantee that the routing table of A is full. To do this we could run either run a spanning tree algorithm or we can update the entries "only when needed". What do I mean with it? 

It means that if A needs to send a message to someone he can perform a broadcast with ack by forwarding his message to B, and at each ack received he can update his routing table.

Or if A does not need to do send messages but is only listening then if node D sends a broadcast the message will arrive to node B which will forward it to node A. And he will update his routing table by saving in the next hop of D the node B.

In this way the routing table still remains static, but one could think about a way to update it dynamically by saying that **each time a new node is added to the routing table it has a Y% probability to establish a direct connection with it**.

**NB:** This works well with applications which ask a broadcast message first. Because with no broadcasts it may happen that node A will never be contacted.

With this method the most important thing is that **the first node to which I connect is also the one with which I exchange my pingpong**. In this way we can construct a "spanning tree" of pingpongs to avoid having too many pings in the network, while ensuring that if some node exits or crashes the apllication can still work properly.

## EXITING

So exiting the network can be either graceful of crashing.

### GRACEFUL

It means that before closing the application the node asks to be removed from the netrwork.

**NB:** what happens if someone wants to send a message to someone who wants to exit the network? It depends.

I guess the best solution is to broadcast a message to ask to be removed from the network, even with no acks should be fine.

Then each node should update accordingly his routing table.

**NB:** We still need to ensure that if a node crashes then the nodes which were pinging with him find a way to "connect" and ping between each other.

### CRASHING

Ok so crashes are a bit trickier because we also need to understand if we want to restore the state of the application. So let's start with the easy: *someone notices that a node has crashed and wants to notify everyone*.

In this case it is easy conceptually but we need to be careful because there could be a network partition. But it does not matter as another node in the other partition will notice it and send to everyone left in the partition that a peer has crashed (then what to do the application decides it).

The most important thing in this case is that **each remaining node in the network is aware of the fact that a node has crashed**. In this way we could have some unexpected behaviors (i.e. messages not delivered, causality not respected, ...) but it does not matter as we will restart from the snapshot when the application is restored.

Or maybe one could resume the exchange of messages after a node has crashed and the network is rebuilt. But we need to ensure that our algorithm is resilient to a lot of disconnections too.

But there are some problems with network partitions, so we need to distinguish the two cases:

* if a node which noticed that the process crashed has no entries in its routing table, after a timer is elapsed (so nobody tried to contact him) then is considered isolated from the network and the **user** needs to manually reconnect to the network.

* if a node is not isolated (i.e. his routing table contains some information) then there are two cases:
* if the node was sending a *PING* (thus receiving a pong) to the crashed process then look for another entry to send the PING (like if it tries to join automatically the network again) and repeat the joining process. **NB:** in the case no node is reachable go to the step above.
* if the node was sending a *PONG* (thus receiving a ping) to the crashed process then it asynchronously waits for someone to ping him and it deletes the entry in the table and does not send pongs back.

In the case of if another peer (not directly connected) wants to contact the old peer an **invalidPeerAddress** message is sent back and can be handled by removing the entry from the routing table.