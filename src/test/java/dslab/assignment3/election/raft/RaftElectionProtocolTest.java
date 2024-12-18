package dslab.assignment3.election.raft;

import dslab.assignment3.election.base.BaseElectionProtocolTest;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.PING;
import static dslab.util.CommandBuilder.PONG;
import static dslab.util.CommandBuilder.declare;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Raft Election Protocol, verifying basic commands and responses.
 *
 * <p>This class extends the base election protocol test class to validate that the Raft election protocol
 * correctly handles the declaration of leaders and ping commands.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class RaftElectionProtocolTest extends BaseElectionProtocolTest {

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void raft_protocol_declare_successfully() throws IOException {
        final int leaderId = config.electionPeerIds()[1];

        sender.connectAndReadResponse();
        assertThat(sender.sendCommandAndReadResponse(declare(leaderId))).contains("ack");
        assertEquals(leaderId, broker.getLeader());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void raft_protocol_ping_successfully() throws IOException {
        sender.connectAndReadResponse();
        assertEquals(PONG, sender.sendCommandAndReadResponse(PING));
    }

    @Override
    public String getElectionType() {
        return "raft";
    }
}
