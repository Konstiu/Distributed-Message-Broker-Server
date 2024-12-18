package dslab.util.mock;

import java.util.Queue;

import static dslab.util.CommandBuilder.OK;
import static dslab.util.CommandBuilder.PING;
import static dslab.util.CommandBuilder.PONG;
import static dslab.util.CommandBuilder.ack;
import static dslab.util.CommandBuilder.declare;
import static dslab.util.CommandBuilder.elect;
import static dslab.util.CommandBuilder.vote;

public class CommandExpectationFactory {

    private final int electionId;
    private final Queue<CommandResponse> expectations;

    public CommandExpectationFactory(int electionId, Queue<CommandResponse> expectations) {
        this.electionId = electionId;
        this.expectations = expectations;
    }

    private CommandExpectationFactory expectCommandReturnResponse(String expectedCommand, String response) {
        this.expectations.add(new CommandResponse(expectedCommand, response));
        return this;
    }

    public CommandExpectationFactory expectElectReturnOk(int id) {
        return expectCommandReturnResponse(elect(id), OK);
    }

    public CommandExpectationFactory expectElectReturnVote(int electId, int candidateId) {
        return expectCommandReturnResponse(elect(electId), vote(electionId, candidateId));
    }

    public CommandExpectationFactory expectDeclareReturnAck(int id) {
        return expectCommandReturnResponse(declare(id), ack(electionId));
    }

    public CommandExpectationFactory expectPingReturnPong() {
        return expectCommandReturnResponse(PING, PONG);
    }
}
