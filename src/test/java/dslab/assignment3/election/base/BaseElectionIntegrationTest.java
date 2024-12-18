package dslab.assignment3.election.base;

import dslab.ComponentFactory;
import dslab.broker.IBroker;
import dslab.config.BrokerConfig;
import dslab.config.DNSServerConfig;
import dslab.dns.IDNSServer;
import dslab.util.ConfigFactory;
import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.Util;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests of broker election mechanisms in a distributed system.
 *
 * <p>This abstract test class sets up a simulated network of brokers and a DNS server,
 * facilitating the testing of election protocols. Concrete subclasses should implement
 * specific tests for broker election scenarios by leveraging the setup and teardown methods
 * defined in this base class.</p>
 */
public abstract class BaseElectionIntegrationTest implements BaseElectionTest, BaseElectionScalableTest {

    protected final int NUM_BROKERS = getNumOfBrokers();
    protected BrokerConfig[] brokerConfigs = new BrokerConfig[NUM_BROKERS];

    protected IBroker[] brokers;
    protected Thread[] brokerThreads;
    protected TreeSet<Integer> brokerIds = new TreeSet<>();

    protected final DNSServerConfig dnsConfig = ConfigFactory.createDNSServerConfig();

    private IDNSServer dnsServer;
    private Thread dnsThread;

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @BeforeEach
    public void beforeEach() throws IOException {
        dnsServer = ComponentFactory.createDNSServer(dnsConfig);
        dnsThread = new Thread(dnsServer);
        dnsThread.start();

        // Create broker configs with randomised election-ids
        brokerIds = Global.SECURE_INT_GENERATOR.getInts(NUM_BROKERS,0,1000);
        Integer[] idsArray = brokerIds.toArray(Integer[]::new);
        brokerConfigs = ConfigFactory.createBrokerConfigsA3(NUM_BROKERS, getElectionType(), idsArray);

        brokers = new IBroker[NUM_BROKERS];
        brokerThreads = new Thread[NUM_BROKERS];

        for (int i = 0; i < brokers.length; i++) {
            brokers[i] = ComponentFactory.createBroker(brokerConfigs[i]);
            brokerThreads[i] = new Thread(brokers[i]);
            brokerThreads[i].start();
        }

        // If this helper connects successfully, then the dns server is ready to accept further connections
        TelnetClientHelper waitForDnsConnHelper = new TelnetClientHelper(Constants.LOCALHOST, dnsConfig.port());
        waitForDnsConnHelper.waitForInitConnection();
        waitForDnsConnHelper.disconnect();

        // Probe if the socket is up and ready for commands
        // If this helper connects successfully, then the broker is ready to accept further connections
        for (int i = 0; i < brokers.length; i++) {
            TelnetClientHelper waitForBrokerConnHelper = new TelnetClientHelper(Constants.LOCALHOST, brokerConfigs[i].electionPort());
            waitForBrokerConnHelper.waitForInitConnection();
            waitForBrokerConnHelper.disconnect();
        }
    }

    @AfterEach
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void afterEach() {
        if (dnsServer != null) {
            dnsServer.shutdown();
        }

        for (int i = 0; i < NUM_BROKERS; i++) {
            if (brokers[i] != null) {
                brokers[i].shutdown();
            }
        }

        try {
            if (dnsThread != null && dnsThread.isAlive()) {
                dnsThread.join();
            }

            for (int i = 0; i < NUM_BROKERS; i++) {
                if (brokerThreads[i] != null && brokerThreads[i].isAlive()) {
                    brokerThreads[i].join();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (BrokerConfig config : brokerConfigs) {
            Util.waitForTcpPortsToClose(config.port(), config.electionPort());
        }

        Util.waitForTcpPortsToClose(dnsConfig.port());
    }

    /**
     * Retrieves the configuration of the broker with the highest ID.
     *
     * <p>This method is used in scenarios where the broker with the highest ID is expected
     * to be the leader. It searches through the list of brokers and returns the configuration
     * of the broker with the highest assigned election ID.</p>
     *
     * @return the {@link BrokerConfig} of the broker with the highest ID, or {@code null} if no leader is found.
     */
    protected BrokerConfig getConfigOfHighestIdBroker() {
        for (int i = 0; i < brokers.length; i++) {
            if (brokers[i].getId() == brokerIds.getLast()) {
                return brokerConfigs[i];
            }
        }

        // No leader
        return null;
    }

    /**
     * Waits for a domain to be resolved by the DNS server and returns the resolution result.
     *
     * <p>Uses the {@link TelnetClientHelper} utility to connect to the DNS server and wait
     * for the registration of the specified domain. This method facilitates DNS resolution
     * testing within the broker election context.</p>
     *
     * @param domain the domain to be resolved.
     * @return the resolved result for the domain.
     * @throws IOException if an error occurs during the DNS resolution process.
     */
    protected String waitForAndResolveDomain(String domain) throws IOException {
        TelnetClientHelper dnsHelper = new TelnetClientHelper(Constants.LOCALHOST, dnsConfig.port());
        dnsHelper.connectAndReadResponse();
        return dnsHelper.waitForDnsRegistration(domain);
    }
}
