# Java library for distributed snapshot

## Task Description

Implement in Java a library that offers the capability of storing a distributed snapshot on disk. Aprropriately design the API of the library for it to be state and message agnostic.

Implement also an application that uses the library to cope with node failures (restarting from the last valid snapshot).

Assumptions:

* Nodes do not crash in the middle of the snapshot
* The topology of the network (including the set of nodes) does not change during a snapshot, but it may change from a snapshot to another.

## Implementation

In order to realize the project the application is divided in two distinct Java Modules:

* [lib-module](./lib-module/) : contains the backend of the application, responsible for the creation of the network and the execution of the distributed snapshot protocol.
* [appExample-module](./appExample-module/) : contains the application built on top of the library. It uses a dedicated API in order to exchange messages, start the snapshot, and restore a valid state.

The two modules are logically distinct, for the purposes of the project we decided to implement a simple distributed chat with state restore. Each peer can send messages to each other in the network and view the state of received messages. 

If one peer accidentally crashes, or the LAN connection is interrupted, the library is halted and no more messages can be exchanged. The only way to recover the network is to restore the crashed node or the LAN network and manually start a snapshot recovery procedure.

For more information on the API and the execution flows, the markdown files [DLIBRARY.md](./DLIBRARY.md) and [MESSAGES.md](./MESSAGES.md) has an in depth description of the component.

Moreover, in the [doc](./doc/) folder we provide images of both the UML and SEQUENCE diagrams which explain visually the library and its components.

## Setup

The application has been built using **Maven** package manager, hence it is recommended to use it paired with the IDE Intellij.

## Testing

Each component is thoroughly tested, both with unit tests and with integration tests. Moreover, we provide a python framework in which is possible to construct simulated networks, and test that the connection is stable and it is possible to send messages. For more information, refer to the dedicated markdown file [test_doc.md](./test_py/testDoc/test_doc.md).

## Building

When building the application with Maven, we create both the jar file for the whole application (with and without dependencies), and the jar file for the standalone distributed snapshot library, which can be exported as a standalone package.

The jar files can be found in `appExample-module/target/` and `lib-module/target/`.

## Run

In order to run the application, the command is simply:

```bash
java -jar dapplication-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Then you will be prompted to insert the IP and port of the application, followed by the possibility of creating a new network or joining an already existing one. If everything is done correctly, it will be possible to exchange messages with each node connected in the same network.

<!-- ## TODO:

We need both the library and the application.

**FIRST**:

* define how the library stores the state of the application
* clarify all the assumptions outside the ones written above
* define all the corner cases (client crashes/connects/disconnects, can be both ethernet & wifi, what do we do? )
* define the public interface of the library
* decide the distributed application for the testing purpose
* start designing the library by thinking about everything it needs to do



| FEATURE | Implemented | Tested             | 
| ---- | ----- |--------------------| 
| Join procedure | :green_circle: | :green_circle:     | 
| Send msg | :green_circle: | :green_circle:     |
| Exit procedure | :green_circle: | :green_circle:     |
| Ping pong | :green_circle: | :green_circle:       |
| Ping pong fail reaction | :green_circle: | :yellow_circle: (manually tested) |
| Crash detection | :green_circle: | :yellow_circle: (manually tested) |
| Snapshot procedure | :green_circle: | :green_circle:     |
| Snapshot procedure reset | :green_circle: | :yellow_circle: (manually tested) | -->



