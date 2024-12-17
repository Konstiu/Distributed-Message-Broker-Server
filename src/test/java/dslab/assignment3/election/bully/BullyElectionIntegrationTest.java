package dslab.assignment3.election.bully;

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
 * Integration tests for the Bully Election Protocol in a broker network.
 *
 * <p>This class tests the behavior of the Bully Election Protocol by simulating scenarios where brokers
 * need to elect a leader and handle leader failures. It extends the base class for election integration
 * tests to leverage shared setup and teardown functionality.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class BullyElectionIntegrationTest extends BaseElectionIntegrationTest {

    @Override
    public String getElectionType() {
        return "bully";
    }

    @Override
    public int getNumOfBrokers() {
        return 3;
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test
    @Timeout(value = 4000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_electsLeader_successfully() throws IOException {
        waitForElectionToEnd(brokers, brokerConfigs[0].electionHeartbeatTimeoutMs());

        for (IBroker broker : brokers) assertThat(broker.getLeader()).isEqualTo(brokerIds.getLast());

        BrokerConfig leaderConfig = getConfigOfHighestIdBroker();

        String[] hostAndPortParts = waitForAndResolveDomain(leaderConfig.electionDomain()).split(":");

        assertAll(
                "Grouped Assertions of User",
                () -> assertEquals(leaderConfig.host(), hostAndPortParts[0]),
                () -> assertEquals(leaderConfig.port(), Integer.parseInt(hostAndPortParts[1]))
        );
    }

    @GitHubClassroomGrading(maxScore = 7)
    @Test
    @Timeout(value = 4000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_currentLeaderShutsDown_electsNewLeader_successfully() throws IOException {

        waitForElectionToEnd(brokers, brokerConfigs[0].electionHeartbeatTimeoutMs());
        for (IBroker broker : brokers) assertThat(broker.getLeader()).isEqualTo(brokerIds.getLast());

        int deadLeaderIndex = ElectionUtil.shutdownLeader(brokers, brokerIds.getLast());
        IBroker[] aliveBrokers = ElectionUtil.getAliveBrokers(brokers, deadLeaderIndex);

        waitForElectionToEnd(aliveBrokers, brokerIds.removeLast(), brokerConfigs[0].electionHeartbeatTimeoutMs());

        for (IBroker aliveBroker : aliveBrokers) assertThat(aliveBroker.getLeader()).isEqualTo(brokerIds.getLast());

        BrokerConfig leaderConfig = getConfigOfHighestIdBroker();

        String[] hostAndPortParts = waitForAndResolveDomain(leaderConfig.electionDomain()).split(":");

        assertAll(
                "Grouped Assertions of User",
                () -> assertEquals(leaderConfig.host(), hostAndPortParts[0]),
                () -> assertEquals(leaderConfig.port(), Integer.parseInt(hostAndPortParts[1]))
        );
    }
}
