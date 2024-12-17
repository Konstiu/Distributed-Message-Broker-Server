package dslab.assignment3.election.ring;

import dslab.assignment3.election.base.BaseElectionIntegrationTest;
import dslab.broker.IBroker;
import dslab.config.BrokerConfig;
import dslab.util.election.ElectionUtil;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.election.ElectionUtil.waitForElectionToEnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the Ring Election protocol, validating the behavior of leader
 * election and recovery in a ring topology.
 *
 * <p>This class extends the base election integration test class and includes tests to ensure
 * that the ring election protocol correctly handles the election process and leader recovery
 * scenarios.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class RingElectionIntegrationTest extends BaseElectionIntegrationTest {

    @Override
    public String getElectionType() {
        return "ring";
    }

    @Override
    public int getNumOfBrokers() {
        return 3;
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test
    @Timeout(value = 4000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_electsLeader_successfully() throws IOException {
        // brokers should run into timeout and elect a leader
        // broker-0 has the lowest timeout and should start the election

        waitForElectionToEnd(brokers, brokerConfigs[0].electionHeartbeatTimeoutMs());

        // All brokers must agree upon a leader (highest id in set "brokerIds")
        for (IBroker broker : brokers) assertThat(broker.getLeader()).isEqualTo(brokerIds.getLast());

        // Get the config of the elected leader
        BrokerConfig leaderConfig = getConfigOfHighestIdBroker();

        // Make sure leader registered himself under the election domain
        String[] hostAndPortParts = waitForAndResolveDomain(leaderConfig.electionDomain()).split(":");

        assertAll(
                () -> assertEquals(leaderConfig.host(), hostAndPortParts[0]),
                () -> assertEquals(leaderConfig.port(), Integer.parseInt(hostAndPortParts[1]))
        );
    }

    @GitHubClassroomGrading(maxScore = 7)
    @Test
    @Timeout(value = 4000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_currentLeaderShutsDown_electsNewLeader_successfully() throws IOException {
        // wait for brokers to init an election via timeout
        // broker-0 has the lowest timeout and should start the election

        waitForElectionToEnd(brokers, brokerConfigs[0].electionHeartbeatTimeoutMs());
        for (IBroker broker : brokers) assertThat(broker.getLeader()).isEqualTo(brokerIds.getLast());

        // shut down the leader (broker with highest id)
        int deadLeaderIndex = ElectionUtil.shutdownLeader(brokers, brokerIds.getLast());
        IBroker[] aliveBrokers = ElectionUtil.getAliveBrokers(brokers, deadLeaderIndex);

        // wait for followers to timeout --> they start a new election
        waitForElectionToEnd(aliveBrokers, brokerIds.removeLast(), brokerConfigs[0].electionHeartbeatTimeoutMs());

        // Make sure that a new leader has been elected
        for (IBroker aliveBroker : aliveBrokers) assertThat(aliveBroker.getLeader()).isEqualTo(brokerIds.getLast());

        // Get the config of the elected leader
        BrokerConfig leaderConfig = getConfigOfHighestIdBroker();

        // Make sure the new leader registered himself under the election domain
        String[] hostAndPortParts = waitForAndResolveDomain(leaderConfig.electionDomain()).split(":");

        assertAll(
                () -> assertEquals(leaderConfig.host(), hostAndPortParts[0]),
                () -> assertEquals(leaderConfig.port(), Integer.parseInt(hostAndPortParts[1]))
        );
    }
}
