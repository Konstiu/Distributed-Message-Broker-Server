package dslab.assignment3.election.bully;

import dslab.assignment3.election.base.BaseElectionProtocolTest;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.OK;
import static dslab.util.CommandBuilder.declare;
import static dslab.util.CommandBuilder.elect;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Bully Election Protocol in the broker network.
 *
 * <p>This class tests the functionality of the Bully Election Protocol by simulating the sending of
 * election and declare commands to verify the broker's response. It extends the base class for
 * election protocol tests to use shared setup and teardown functionalities.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class BullyElectionProtocolTest extends BaseElectionProtocolTest {

    @Override
    public String getElectionType() {
        return "bully";
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_elect_successfully() throws IOException {
        sender.connectAndReadResponse();
        assertEquals(OK, sender.sendCommandAndReadResponse(elect(config.electionPeerIds()[1])));
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void bully_declare_successfully() throws IOException {
        final int leaderId = config.electionPeerIds()[1];

        sender.connectAndReadResponse();
        assertThat(sender.sendCommandAndReadResponse(declare(leaderId))).contains("ack");
        assertEquals(leaderId, broker.getLeader());
    }
}
