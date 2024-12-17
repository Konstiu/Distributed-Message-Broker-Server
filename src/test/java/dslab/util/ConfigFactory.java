package dslab.util;

import dslab.config.BrokerConfig;
import dslab.config.DNSServerConfig;
import dslab.config.MonitoringServerConfig;

public class ConfigFactory {

    public static MonitoringServerConfig createMonitoringServerConfig() {
        return new MonitoringServerConfig("monitoring-test", Constants.MONITORING_PORT);
    }

    public static DNSServerConfig createDNSServerConfig() {
        return new DNSServerConfig("dns-0", Constants.DNS_PORT);
    }

    public static BrokerConfig createBrokerConfigA3(String electionType, int electionId, int[] electionPeerPorts, int[] electionPeerIds) {

        return new BrokerConfig("broker-%d".formatted(0),
                Constants.LOCALHOST,
                Constants.BROKER_BASE_PORT,
                Constants.LOCALHOST,
                Constants.DNS_PORT,
                "broker-%d.at".formatted(0),
                electionId,
                electionType,
                Constants.BROKER_BASE_PORT + 1,
                "election.brokers.at",
                new String[]{Constants.LOCALHOST, Constants.LOCALHOST},
                electionPeerPorts,
                electionPeerIds,
                Constants.GLOBAL_BASE_ELECTION_HEARTBEAT_TIMEOUT_MS,
                Constants.LOCALHOST,
                Constants.MONITORING_PORT
        );
    }

    public static BrokerConfig createBrokerConfigA2() {
        return createBrokerConfigsA2(1)[0];
    }

    public static BrokerConfig[] createBrokerConfigsA2(int numBrokers) {
        BrokerConfig[] configs = new BrokerConfig[numBrokers];

        for (int id = 0; id < numBrokers; id++) {
            int offset = 10 * id; // offset to where the next broker range starts
            int brokerPort = Constants.BROKER_BASE_PORT + offset;

            configs[id] = new BrokerConfig(
                    "broker-%d".formatted(id),
                    Constants.LOCALHOST,
                    brokerPort,
                    Constants.LOCALHOST,
                    Constants.DNS_PORT,
                    "broker-%d.at".formatted(id),
                    0,
                    "none",
                    0,
                    "",
                    new String[]{},
                    new int[]{},
                    new int[]{},
                    0,
                    "",
                    0
            );
        }

        return configs;
    }

    public static BrokerConfig[] createBrokerConfigsA3(int numBrokers, String electionType, Integer[] electionIds) {
        BrokerConfig[] configs = new BrokerConfig[numBrokers];

        final int numPeers = numBrokers - 1;

        for (int id = 0; id < numBrokers; id++) {
            int offset = 10 * id; // offset to where the next broker range starts
            int brokerPort = Constants.BROKER_BASE_PORT + offset;
            int electionPort = brokerPort + 1;

            int[] electionPeerIds = new int[numPeers];
            int[] electionPeerPorts = new int[numPeers];
            String[] electionPeerHosts = new String[numPeers];
            for (int i = 0; i < numPeers; i++) {
                int peerId = (id + i + 1) % numBrokers;
                int peerPortOffset = peerId * 10;
                electionPeerPorts[i] = (Constants.BROKER_BASE_PORT + 1) + peerPortOffset;
                electionPeerHosts[i] = Constants.LOCALHOST;
                electionPeerIds[i] = electionIds[(id + 1 + i) % numBrokers];
            }

            configs[id] = new BrokerConfig(
                    "broker-%d".formatted(id),
                    Constants.LOCALHOST,
                    brokerPort,
                    Constants.LOCALHOST,
                    Constants.DNS_PORT,
                    "broker-%d.at".formatted(id),
                    electionIds[id],
                    electionType,
                    electionPort,
                    "election.brokers.at",
                    electionPeerHosts,
                    electionPeerPorts,
                    electionPeerIds,
                    Constants.GLOBAL_BASE_ELECTION_HEARTBEAT_TIMEOUT_MS + 100L * id,
                    Constants.LOCALHOST,
                    Constants.MONITORING_PORT
            );
        }

        return configs;
    }

}
