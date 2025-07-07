<!--[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/cFyffb2i)
# Assignment 3: DSLab Leader Election and UDP Monitoring
This README serves as description for the project repository, examples of protocol responses and some information about
testing.
Please read the assignment description to find more details about the servers you have to implement and further
instructions.
-->
# Distributed Message Broker Service
![image](https://github.com/user-attachments/assets/ee75df25-ad37-48a9-8c74-7270f26107e1)


## Description
<!--*Link to client application: [https://github.com/nikiblauer/Distributed-Message-Broker-Service-Client](https://github.com/nikiblauer/Distributed-Message-Broker-Service-Client)*-->

This code implements a distributed message broker service that follows a simplified version of the AMQP protocol. The system is designed to support multiple message brokers running simultaneously, each registering itself with a custom DNS service to maintain an updated view of the broker cluster.

A message broker acts as an intermediary between clients, handling their requests and ensuring reliable message delivery. It manages exchanges and queues, which are core components of the message routing process. Clients can publish messages to an exchange, which then routes these messages to one or more queues based on predefined rules and routing keys. Other clients can bind to these queues to receive messages asynchronously, enabling scalable and decoupled communication between producers and consumers.

To ensure high availability and fault tolerance, the system includes a mechanism for automatic leader election. If a message broker fails, the remaining brokers detect the failure and initiate a leader election process using one of three algorithms: Raft, Ring, or Bully. Once a new leader is elected, it takes over the coordination responsibilities and updates the DNS service to reflect the current state of the broker cluster. This dynamic leadership transition ensures that message routing and delivery continue without interruption, maintaining system stability and reliability.

By distributing the workload across multiple brokers and implementing robust failure recovery mechanisms, this service provides a scalable and resilient messaging infrastructure for distributed applications.

*Link to client application: [[https://github.com/Konstiu/Distributed-Message-Broker-Client](https://github.com/Konstiu/Distributed-Message-Broker-Client)]*

## Table of Contents
- [General](#General)
- [Protocols](#Protocols)
- [Testing](#Testing)

# General

<!--## GitHub Classroom Grading

To grade this assignment, we will use GitHub Actions to automatically build and test the code submitted by students.
After pushing your
solution to the GitHub repository, the GitHub Actions will run automatically and provide feedback on the correctness of
the solution.
The grading feedback is provided at the end of the execution of the GitHub Actions Workflow. The feedback will contain
information about
each test case and whether it passed successfully or failed. If a test case failed, you will be able to see the error
output in the corresponding
execution step.-->

## Java Version
Use `Java JDK-21`. The Github-runner uses `Oracle Java JDK 21` to run your tests. Make sure your code is compatible

## Starting the Server Applications

```bash
# First compile the project with Maven
mvn compile
# Start the client application with the following command where componentId is one of client-0, client-1 or client-2.
mvn exec:java@<componentId>
# You can also combine both commands into one
mvn compile exec:java@<componentId>
```

You need to replace `<componentId>` with the corresponding component ID of the server: `broker-0`, `broker-1`,
`broker-2` and `dns-0` are available.
You may need multiple terminal windows to start multiple servers.

The default configuration for the servers can be started using:

1. `mvn compile`
2. `mvn package -DskipTests`
3. Run selected server
- `mvn exec:java@broker-0`
- `mvn exec:java@broker-1`
- `mvn exec:java@broker-2`
- `mvn exec:java@monitoring-0`
- `mvn exec:java@dns-0`

<!--## Protect Files and Directories
The following files and directories are protected and should not be modified by students since this will flag the submission with a `warning`:

- `.github/**/*` which is used for the GitHub Actions Workflows (e.g. Classroom Autograding-)
- `src/main/resources/**/*` which contains all the configuration files for this assignment-
- `src/test/**/*` which contains all the tests and further helper classes for this assignment-
- `pom.xml` which defines all necessary external dependencies for this assignment and is used to build and test the project with GitHub Actions

## Assignment Structure
The structure of the assignment is as follows:

- `src/main/java/**/*` contains all the source code for this assignment
- `src/main/test/**/*` contains all the tests and further helper classes for this assignment
-->
## Threads types
Consider using Java Virtual Threads for  short-lived threads and client-handlers for better i/o performance and less expensive thread creation.

# Protocols

## Leader Election Protocol (LEP)
Upon connecting to a Broker leader election port, the server sends the greeting `ok LEP`.

### `elect <id>`
Notifies that the broker with the given `id` is up for election.
#### Responses
| State                | Response                          |
|----------------------|-----------------------------------|
| success (ring/bully) | `ok`                              |
| success (raft)       | `vote <sender-id> <candidate-id>` |
| error syntax         | `error usage: elect <id>`         |

### `declare <id>`
Declares the broker with the given `ìd` as the new leader.
#### Responses
| State        | Response                    |
|--------------|-----------------------------|
| success      | `ack <sender-id>`           |
| error syntax | `error usage: declare <id>` |

### `ping`
Sent by the leader to the followers as a heartbeat If the followers do not receive
a heartbeat message within a set timeout, a new leader election is started.
#### Responses
| State        | Response               |
|--------------|------------------------|
| success      | `pong`                 |
| error syntax | `error protocol error` |

### Default Response
If no matching command of the protocol is found, then the broker sends `error protocol error` and closes the connection

### `vote <sender-id> <candidate-id>`
The sender with `sender-id` votes the for the candidate with `candidate-id`

# Testing
This paragraph describes the default testing configuration for the available server (message-broker, dns-server, monitoring-server).

## Naming scheme
The names of the servers start with `0` and are incremented by `1` every new node.

- Message Broker: `broker-#`
- DNS Server: `dns-#`
- Monitoring Server: `monitoring-#`

For example the available servers for
a test-case verifying that after a raft-leader election, the leader registers its domain at the dns-server could look like this:

- `dns-0`
- `broker-0`
- `broker-1`
- `broker-2`
- `broker-3`

### Message Broker
The `broker-0` listens for publisher/subscribers on TCP port `20000` and for election updates from other brokers on
TCP port `20001`. These 2 ports are incremented by 10 for every new broker instance. So for example for the first 3
brokers we would have:

- `broker-0`: messaging=`20000`, election=`20001`
- `broker-1`: messaging=`20010`, election=`20011`
- `broker-2`: messaging=`20020`, election=`20021`

### DNS server
The `dns-0` listens per default on TCP port `18000`.

### Monitoring server
The `monitoring-0` listens per default on UDP port `17000`.

## Using netcat for Manual Testing

You can use netcat (nc) to connect to the server and send commands manually. To start a netcat client that connects to
the server at `localhost` on port `20000`, you can use the following commands:

```bash
# For Linux and macOS
# Open the terminal. The following command starts a netcat client that connects to the server at localhost on port 20000.
nc localhost 20000
```

```bash
# For Windows first ensure that you have installed netcat (ncat). If not, you can download it from the following link: https://nmap.org/download.html#windows
# Open CMD or PowerShell. The following command starts a netcat client that connects to the server at localhost on port 20000.
ncat -C localhost 20000
```
