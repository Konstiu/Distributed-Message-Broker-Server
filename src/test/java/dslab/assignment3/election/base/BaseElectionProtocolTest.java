package dslab.assignment3.election.base;

import dslab.ComponentFactory;
import dslab.broker.IBroker;
import dslab.config.BrokerConfig;
import dslab.util.ConfigFactory;
import dslab.util.Constants;
import dslab.util.Util;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests of the election protocol in a broker network.
 *
 * <p>This abstract test class is used to set up a single broker and simulate election protocol scenarios
 * by establishing the necessary components and connections. Concrete subclasses should implement specific
 * tests for various election behaviors.</p>
 */
public abstract class BaseElectionProtocolTest implements BaseElectionTest {

    protected static final int BROKER_ELECTION_ID = 10;
    protected final BrokerConfig config = ConfigFactory.createBrokerConfigA3(
            getElectionType(),
            BROKER_ELECTION_ID,
            new int[]{20011, 20021},
            new int[]{BROKER_ELECTION_ID + 1, BROKER_ELECTION_ID + 2}
    );

    protected IBroker broker;
    protected Thread brokerThread;

    protected TelnetClientHelper sender;

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @BeforeEach
    public void beforeEach() throws IOException {
        broker = ComponentFactory.createBroker(config);

        // Run the brokers after the DNS Server is up, so the DNS registration can happen with rudimentary implementation
        brokerThread = new Thread(broker);
        brokerThread.start();

        // Probe if the socket is up and ready for commands
        // If this helper connects successfully, then the broker is ready to accept further connections
        TelnetClientHelper waitForBrokerConnHelper = new TelnetClientHelper(Constants.LOCALHOST, config.electionPort());
        waitForBrokerConnHelper.waitForInitConnection();
        waitForBrokerConnHelper.disconnect();

        // Create helper and receiver to mimic adjacent Nodes in the ring
        sender = new TelnetClientHelper(Constants.LOCALHOST, config.electionPort());
    }

    @AfterEach
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void afterEach() {
        if (broker != null) {
            broker.shutdown();
        }

        try {
            if (brokerThread != null && brokerThread.isAlive()) {
                brokerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Util.waitForTcpPortsToClose(config.port(), config.electionPort());
    }
}
