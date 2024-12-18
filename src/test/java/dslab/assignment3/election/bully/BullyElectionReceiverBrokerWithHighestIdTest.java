package dslab.assignment3.election.bully;

import dslab.assignment3.election.base.BaseElectionReceiverTest;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import dslab.util.mock.MockServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.PING;
import static dslab.util.CommandBuilder.declare;
import static dslab.util.CommandBuilder.elect;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the Bully Election Protocol, specifically testing scenarios where the broker with the highest ID is expected to become the leader.
 *
 * <p>This class extends the base election receiver test class to validate interactions between brokers and mock receivers during the election process.
 * It ensures that brokers initiate elections, declare themselves as leaders, and notify peers accordingly.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class BullyElectionReceiverBrokerWithHighestIdTest extends BaseElectionReceiverTest {

    @Override
    protected int[] createReceiverIDs(int numOfReceivers) {
        int[] ids = new int[numOfReceivers];
        for (int i = 0; i < numOfReceivers; i++) {
            ids[i] = BROKER_ELECTION_ID - i - 1;
        }

        return ids;
    }

    @Override
    public String getElectionType() {
        return "bully";
    }

    @Override
    protected int getNumOfReceivers() {
        return 2;
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_initiatesElection_sendsElectMessageToPeers_becomesLeader() throws InterruptedException {
        for (MockServer receiver : receivers) receiver.expect().expectDeclareReturnAck(BROKER_ELECTION_ID);

        broker.initiateElection();

        for (int i = 0; i < numOfReceivers; i++) assertEquals(declare(BROKER_ELECTION_ID), receivers[i].takeFromReceivedCommands());

        assertEquals(BROKER_ELECTION_ID, broker.getLeader());
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_receivesElectOfLowerId_initiatesNewElection_becomesLeader_sendsDeclare() throws IOException, InterruptedException {
        for (MockServer receiver : receivers) receiver.expect().expectDeclareReturnAck(BROKER_ELECTION_ID);

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(elect(BROKER_ELECTION_ID - 1));

        for (int i = 0; i < numOfReceivers; i++) assertEquals(declare(BROKER_ELECTION_ID), receivers[i].takeFromReceivedCommands());

        assertEquals(BROKER_ELECTION_ID, broker.getLeader());
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_becomesLeader_startsSendingHeartbeatsToPeers() throws InterruptedException {
        for (MockServer receiver : receivers) {
            receiver.expect()
                    .expectDeclareReturnAck(BROKER_ELECTION_ID)
                    .expectPingReturnPong();
        }

        broker.initiateElection();

        for (MockServer server : receivers) assertEquals(declare(BROKER_ELECTION_ID), server.takeFromReceivedCommands());

        for (MockServer mockServer : receivers) assertEquals(PING, mockServer.takeFromReceivedCommands());
    }

}
