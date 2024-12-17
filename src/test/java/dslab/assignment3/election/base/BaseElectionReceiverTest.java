package dslab.assignment3.election.base;

import dslab.ComponentFactory;
import dslab.broker.IBroker;
import dslab.config.BrokerConfig;
import dslab.util.*;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests of the broker election protocol with multiple receivers.
 *
 * <p>This abstract test class is used to set up a broker and multiple receiver nodes for testing election
 * protocol scenarios in a distributed system. Concrete subclasses should implement specific tests for
 * different election behaviors.</p>
 */
public abstract class BaseElectionReceiverTest implements BaseElectionTest {

    protected static final int BROKER_ELECTION_ID = Global.SECURE_INT_GENERATOR.getInt(10, 100);

    protected final int numOfReceivers = getNumOfReceivers();

    protected final int[] receiverPorts = createReceiverPorts(numOfReceivers);
    protected final int[] receiverIds = createReceiverIDs(numOfReceivers);

    protected final BrokerConfig config = ConfigFactory.createBrokerConfigA3(getElectionType(), BROKER_ELECTION_ID, receiverPorts, receiverIds);

    protected IBroker broker;
    protected Thread brokerThread;

    // Helper and receiver to mimic adjacent Nodes in the ring
    protected MockServer[] receivers;
    protected Thread[] receiverThreads;
    protected TelnetClientHelper sender;

    // Points to receivers[0], used for simpler testing
    protected MockServer receiver;

    /**
     * Abstract method to be implemented by subclasses to provide the number of receivers to be created.
     *
     * @return the number of receivers.
     */
    protected abstract int getNumOfReceivers();

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @BeforeEach
    public void beforeEach() throws IOException {
        broker = ComponentFactory.createBroker(config);
        brokerThread = new Thread(broker);
        brokerThread.start();

        receivers = new MockServer[numOfReceivers];
        receiverThreads = new Thread[numOfReceivers];
        for (int i = 0; i < numOfReceivers; i++) {
            receivers[i] = new MockServer(receiverPorts[i], receiverIds[i]);
            receiverThreads[i] = new Thread(receivers[i]);
            receiverThreads[i].start();
        }

        // Set a reference to the first receiver to make testing easier
        receiver = receivers[0];

        // Probe if the socket is up and ready for commands
        // If this helper connects successfully, then the broker is ready to accept further connections
        TelnetClientHelper waitForConnHelper = new TelnetClientHelper(Constants.LOCALHOST, config.electionPort());
        waitForConnHelper.waitForInitConnection();
        waitForConnHelper.disconnect();

        // If this helper connects successfully, then the mock server is ready to accept further connections
        for (int i = 0; i < numOfReceivers; i++) {
            waitForConnHelper = new TelnetClientHelper(Constants.LOCALHOST, receiverPorts[i]);
            waitForConnHelper.waitForInitConnection();
            waitForConnHelper.disconnect();
        }

        // Create helper and receiver to mimic adjacent Nodes in the ring
        sender = new TelnetClientHelper(Constants.LOCALHOST, config.electionPort());
    }

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @AfterEach
    public void afterEach() {
        receiver = null; // clear reference to receiver-0

        if (broker != null) {
            broker.shutdown();
        }

        for (MockServer mockServer : receivers) {
            if (mockServer != null) {
                mockServer.shutdown();
            }
        }

        try {
            if (brokerThread != null && brokerThread.isAlive()) {
                brokerThread.join();
            }

            for (Thread receiverThread : receiverThreads) {
                if (receiverThread != null && receiverThread.isAlive()) {
                    receiverThread.join();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Util.waitForTcpPortsToClose(config.port());
        Util.waitForTcpPortsToClose(receiverPorts);
    }

    /**
     * Creates an array of ports for the receivers based on the number of receivers.
     *
     * @param numOfReceivers the number of receiver nodes.
     * @return an array of port numbers for the receivers.
     */
    protected int[] createReceiverPorts(int numOfReceivers) {
        int[] ports = new int[numOfReceivers];
        for (int i = 0; i < numOfReceivers; i++) {
            ports[i] = Constants.BROKER_BASE_PORT + 11 + 10 * i;
        }

        return ports;
    }

    /**
     * Creates an array of unique IDs for the receivers based on the number of receivers.
     *
     * @param numOfReceivers the number of receiver nodes.
     * @return an array of unique IDs for the receivers.
     */
    protected int[] createReceiverIDs(int numOfReceivers) {
        int[] ids = new int[numOfReceivers];
        for (int i = 0; i < numOfReceivers; i++) {
            ids[i] = BROKER_ELECTION_ID + i + 1;
        }

        return ids;
    }

}
