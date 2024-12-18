package dslab.assignment3.election.raft;

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
 * Integration tests for the Raft Election Protocol, verifying the election process among multiple brokers. Tests if
 * the newly elected leader registers its domain via the DNS server.
 *
 * <p>This class extends the base election integration test class to validate that the Raft election protocol correctly
 * elects leaders and handles leader failures within a distributed broker system.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class RaftElectionIntegrationTest extends BaseElectionIntegrationTest {
    @Override
    public String getElectionType() {
        return "raft";
    }

    @Override
    public int getNumOfBrokers() {
        return 4;
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test
    @Timeout(value = 4000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void raft_electsLeader_successfully() throws IOException {
        waitForElectionToEnd(brokers, brokerConfigs[0].electionHeartbeatTimeoutMs());

        for (IBroker broker : brokers) {
            assertThat(broker.getLeader()).isEqualTo(brokers[0].getId());
            assertThat(brokerIds).contains(broker.getLeader());
        }

        BrokerConfig leaderConfig = brokerConfigs[0];

        String[] hostAndPortParts = waitForAndResolveDomain(leaderConfig.electionDomain()).split(":");

        assertAll(
                () -> assertEquals(leaderConfig.host(), hostAndPortParts[0]),
                () -> assertEquals(leaderConfig.port(), Integer.parseInt(hostAndPortParts[1]))
        );
    }

    @GitHubClassroomGrading(maxScore = 7)
    @Test
    @Timeout(value = 4000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void raft_currentLeaderShutsDown_electsNewLeader_successfully() throws  IOException {
        waitForElectionToEnd(brokers, brokerConfigs[0].electionHeartbeatTimeoutMs());

        for (IBroker broker : brokers) assertThat(broker.getLeader()).isEqualTo(brokers[0].getId());

        brokers[0].shutdown();
        IBroker[] aliveBrokers = ElectionUtil.getAliveBrokers(brokers, 0);

        waitForElectionToEnd(aliveBrokers, brokers[0].getId(), brokerConfigs[1].electionHeartbeatTimeoutMs());

        for (IBroker aliveBroker : aliveBrokers) assertThat(aliveBroker.getLeader()).isEqualTo(brokers[1].getId());

        BrokerConfig leaderConfig = brokerConfigs[1];

        String[] hostAndPortParts = waitForAndResolveDomain(leaderConfig.electionDomain()).split(":");

        assertAll(
                () -> assertEquals(leaderConfig.host(), hostAndPortParts[0]),
                () -> assertEquals(leaderConfig.port(), Integer.parseInt(hostAndPortParts[1]))
        );
    }
}
