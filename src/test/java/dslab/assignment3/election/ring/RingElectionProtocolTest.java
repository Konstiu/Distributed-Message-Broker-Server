package dslab.assignment3.election.ring;

import dslab.assignment3.election.base.BaseElectionProtocolTest;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.OK;
import static dslab.util.CommandBuilder.PING;
import static dslab.util.CommandBuilder.PONG;
import static dslab.util.CommandBuilder.declare;
import static dslab.util.CommandBuilder.elect;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Ring Election protocol, focusing on the command responses
 * and the correct functioning of the election commands.
 *
 * <p>This class extends the base election protocol test class and includes tests
 * that verify the proper behavior of connection handling, election command syntax,
 * declaration of leaders, and ping responses within the Ring Election protocol.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class RingElectionProtocolTest extends BaseElectionProtocolTest {

    @Override
    public String getElectionType() {
        return "ring";
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_protocol_connectsReceivesGreeting_successfully() throws IOException {
        assertEquals("ok LEP", sender.connectAndReadResponse());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_protocol_elect_successfully() throws IOException {
        sender.connectAndReadResponse();
        assertEquals(OK, sender.sendCommandAndReadResponse(elect(10)));
    }

    @GitHubClassroomGrading(maxScore = 2)
    @ParameterizedTest
    @ValueSource(strings = {"elect 10 20", "elect"})
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_protocol_elect_invalidSyntax(String message) throws IOException {
        sender.connectAndReadResponse();

        String res = sender.sendCommandAndReadResponse(message);
        assertThat(res).startsWith("error");
        assertThat(res).contains("usage", "elect");
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_protocol_declare_successfully() throws IOException {
        final int leaderId = config.electionPeerIds()[1];

        sender.connectAndReadResponse();
        assertThat(sender.sendCommandAndReadResponse(declare(leaderId))).contains("ack");
        assertEquals(leaderId, broker.getLeader());
    }

    @GitHubClassroomGrading(maxScore = 2)
    @ParameterizedTest
    @ValueSource(strings = {"declare 10 20", "declare"})
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_protocol_declare_invalidSyntax(String message) throws IOException {
        sender.connectAndReadResponse();

        String res = sender.sendCommandAndReadResponse(message);
        assertThat(res).startsWith("error");
        assertThat(res).contains("usage", "declare");
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ring_protocol_ping_successfully() throws IOException {
        sender.connectAndReadResponse();
        assertEquals(PONG, sender.sendCommandAndReadResponse(PING));
    }
}
