package dslab.assignment3.election.bully;

import dslab.assignment3.election.base.BaseElectionReceiverTest;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.declare;
import static dslab.util.CommandBuilder.elect;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the Bully Election Protocol, specifically testing scenarios where the broker has a lower ID than its peers.
 * This means that the message broker should never become the leader.
 *
 * <p>This class extends the base election receiver test class to validate interactions between brokers and mock receivers
 * when the broker has a lower ID. It ensures that brokers do not erroneously initiate elections and correctly handle incoming
 * election messages.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class BullyElectionReceiverBrokerWithLowerIdTest extends BaseElectionReceiverTest {

    @Override
    public String getElectionType() {
        return "bully";
    }

    @Override
    protected int getNumOfReceivers() {
        return 2;
    }

    @Override
    protected int[] createReceiverIDs(int numOfReceivers) {
        // receiver-0 should receive elect messages
        // receiver-1 should not receive elect messages its id is lower than the Message Broker's
        int[] ids = new int[numOfReceivers];
        ids[0] = BROKER_ELECTION_ID + 1;
        for (int i = 1; i < numOfReceivers; i++) {
            ids[i] = BROKER_ELECTION_ID - 1;
        }

        return ids;
    }


    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_initiatesElection_initiatesNewElection_doesNotBecomeLeader() throws InterruptedException {
        receivers[0].expect().expectElectReturnOk(BROKER_ELECTION_ID);

        broker.initiateElection();

        // only receivers-0 should receive a elect statement (receiver-0 has a higher id)
        assertEquals(elect(BROKER_ELECTION_ID), receivers[0].takeFromReceivedCommands());
        assertThat(receivers[1].getReceivedCommandsCount()).isEqualTo(0);

        assertThat(broker.getLeader()).isLessThan(0);
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_receivesElectOfHigherId_doesNotInitiateElection() throws IOException {
        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(elect(BROKER_ELECTION_ID + 1));

        for (int i = 0; i < numOfReceivers; i++) assertEquals(0, receiver.getReceivedCommandsCount());
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_receivesElectOfLowerId_initiatesNewElection_doesNotBecomeLeader() throws IOException, InterruptedException {
        receivers[0].expect().expectElectReturnOk(BROKER_ELECTION_ID);

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(elect(BROKER_ELECTION_ID - 1));

        assertEquals(elect(BROKER_ELECTION_ID), receivers[0].takeFromReceivedCommands());
        assertThat(receivers[1].getReceivedCommandsCount()).isEqualTo(0);

        assertThat(broker.getLeader()).isLessThan(0);
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_receivesDeclare_setsLeaderId() throws IOException {
        final int newLeaderId = BROKER_ELECTION_ID + 1;

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(declare(newLeaderId));

        assertEquals(newLeaderId, broker.getLeader());
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_reachesTimeout_initiatesNewElection() throws InterruptedException {
        receivers[0].expect().expectElectReturnOk(BROKER_ELECTION_ID);

        assertEquals(elect(BROKER_ELECTION_ID), receivers[0].takeFromReceivedCommands());
        assertThat(receivers[1].getReceivedCommandsCount()).isEqualTo(0);
    }
}
