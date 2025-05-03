# Java library for distributed snapshot

## Description

Implement in Java a library that offers the capability of storing a distributed snapshot on disk. Aprropriately design the API of the library for it to be state and message agnostic.

Implement also an application that uses the library to cope with node failures (restarting from the last valid snapshot).

Assumptions:

* Nodes do not crash in the middle of the snapshot
* The topology of the network (including the set of nodes) does not change during a snapshot, but it may change from a snapshot to another.

## TODO:

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
| Ping pong | :green_circle: | :red_circle:       |
| Ping pong fail reaction | :red_circle: | :red_circle:       |
| Snapshot procedure | :green_circle: | :green_circle:     |
| Snapshot procedure reset | :green_circle: | :yellow_circle: (manually tested) |



