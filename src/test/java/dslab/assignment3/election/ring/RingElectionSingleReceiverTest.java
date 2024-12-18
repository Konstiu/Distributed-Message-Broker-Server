package dslab.assignment3.election.ring;

import dslab.assignment3.election.base.BaseElectionReceiverTest;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static dslab.util.CommandBuilder.declare;
import static dslab.util.CommandBuilder.elect;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Ring Election protocol with a single receiver.
 *
 * <p>This class extends the base election receiver test and includes tests
 * that validate the behavior of the ring election protocol, focusing on
 * the actions taken by the master broker (MB) when it receives election
 * messages and declares leaders.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class RingElectionSingleReceiverTest extends BaseElectionReceiverTest {

    @Override
    public String getElectionType() {
        return "ring";
    }


    @Override
    protected int getNumOfReceivers() {
        return 1;
    }

    public static Stream<Arguments> source_ring_receivesElectOfDifferentId_forwardsElect() {
        return Stream.of(
                // MB receives lower id --> propagate your own id
                Arguments.of(elect(BROKER_ELECTION_ID - 1), BROKER_ELECTION_ID),
                // MB receives higher id --> propagate received id
                Arguments.of(elect(BROKER_ELECTION_ID + 1), BROKER_ELECTION_ID + 1)
        );
    }

    @GitHubClassroomGrading(maxScore = 1)
    @ParameterizedTest
    @MethodSource("source_ring_receivesElectOfDifferentId_forwardsElect")
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_receivesElectOfDifferentId_forwardsElect(String msgSendToBroker, int expectedElectionId) throws IOException, InterruptedException {
        receiver.expect().expectElectReturnOk(expectedElectionId);

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(msgSendToBroker);

        assertEquals(elect(expectedElectionId), receiver.takeFromReceivedCommands());
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_receivesElectOfOwnId_forwardsDeclare() throws IOException, InterruptedException {
        receiver.expect().expectDeclareReturnAck(BROKER_ELECTION_ID);

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(elect(BROKER_ELECTION_ID));

        assertEquals(declare(BROKER_ELECTION_ID), receiver.takeFromReceivedCommands());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_receivesElectOfOwnId_setsItselfAsLeader() throws IOException {
        receiver.expect().expectDeclareReturnAck(BROKER_ELECTION_ID);

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(elect(BROKER_ELECTION_ID));
        // IMPORTANT: make sure to respond with "ok" after the leader has been set. Otherwise this test might fail
        // due to concurrency

        assertEquals(BROKER_ELECTION_ID, broker.getLeader());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_receivesDeclareOfDifferentId_setsReceivedIdAsLeader() throws IOException {
        final int leaderId = BROKER_ELECTION_ID + 1;

        receiver.expect().expectDeclareReturnAck(leaderId); // MB might forward declare msg to next peer in the ring

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(declare(leaderId));
        // IMPORTANT: make sure to respond with "ack <sender-id>" after the leader has been set. Otherwise this test might fail
        // due to concurrency

        assertEquals(leaderId, broker.getLeader());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_receivesDeclareOfHigherId_forwardsDeclare() throws IOException, InterruptedException {
        final int leaderId = BROKER_ELECTION_ID + 1;
        receiver.expect().expectDeclareReturnAck(leaderId);

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(declare(leaderId));

        // Check that MB forwarded the declare msg
        assertEquals(declare(leaderId), receiver.takeFromReceivedCommands());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_receivesDeclareOfOwnId_doesNotForwardDeclare() throws IOException {
        final int leaderId = BROKER_ELECTION_ID;
        receiver.expect().expectDeclareReturnAck(leaderId);

        sender.connectAndReadResponse();
        sender.sendCommandAndReadResponse(declare(leaderId));

        // Check that MB did not forward the declare msg
        assertEquals(0, receiver.getReceivedNonHeartbeatCommandsCount());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_initiatesElection_sendsElectMessageToNextPeer() throws InterruptedException {
        receiver.expect().expectElectReturnOk(BROKER_ELECTION_ID);

        broker.initiateElection();

        // Check that MB send elect message to peer
        assertEquals(elect(BROKER_ELECTION_ID), receiver.takeFromReceivedCommands());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_reachesTimeout_initiatesNewElection() throws InterruptedException {
        final int leaderId = BROKER_ELECTION_ID;
        receiver.expect().expectElectReturnOk(leaderId);

        // broker should realise that there is no leader by running into the timeout and start a new election

        // Check that MB did not forward the declare msg
        assertEquals(elect(leaderId), receiver.takeFromReceivedCommands());
    }
}
