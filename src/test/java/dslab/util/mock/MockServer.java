package dslab.util.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static dslab.util.CommandBuilder.PING;
import static dslab.util.CommandBuilder.PONG;

/**
 * Only supports a single connection
 */
public class MockServer implements Runnable {

    private static final String ERROR_INVALID_MESSAGE = "INVALID MESSAGE RECEIVED";
    private static final String ERROR_NO_COMMAND_EXPECTED = "NO COMMAND WAS EXPECTED";
    private static final String ERROR_NO_EXPECTATIONS = "NO EXPECTATIONS SET";


    private final BlockingQueue<String> receivedCommands = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> receivedNonHeartbeatCommands = new LinkedBlockingQueue<>();
    private Queue<CommandResponse> expectations;
    private final int electionPort;
    private final int electionId;

    private ServerSocket socket;

    public MockServer(int electionPort, int electionId) {
        this.electionPort = electionPort;
        this.electionId = electionId;
    }

    @Override
    public void run() {
        try (ServerSocket socket = new ServerSocket(electionPort)) {
            this.socket = socket;

            while (!socket.isClosed()) {
                Socket s = socket.accept();
                handleConnection(s);
            }
        } catch (IOException e) {
            // ignored
        }
    }

    private void handleConnection(Socket connection) {
        try (
                connection;
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                PrintStream out = new PrintStream(connection.getOutputStream(), true)
        ) {

            out.println("ok LEP");
            boolean ok = true;
            while (!connection.isClosed() && ok) {
                ok = handleInput(in, out);
            }

        } catch (IOException e) {
            // ignored
        }
    }

    private boolean handleInput(BufferedReader in, PrintStream out) throws IOException {
        String read = in.readLine();

        if (expectations == null) {
            out.println(ERROR_NO_EXPECTATIONS);
            return false;
        }

        if (read == null || read.isBlank()) return false;

        CommandResponse c = expectations.poll();

        receivedCommands.add(read);
        if (!read.equals(PING) && !read.equals(PONG)) receivedNonHeartbeatCommands.add(read);

        // Always answer to a ping
        if (read.equals(PING)) {
            out.println(PONG);
            return true;
        }

        // Check if expectations are exhausted
        if (c == null) {
            out.println(ERROR_NO_COMMAND_EXPECTED);
            return false;
        }

        if (!read.equals(c.expectation())) {
            out.println(ERROR_INVALID_MESSAGE);
            return false;
        }

        out.println(c.response());
        return true;
    }

    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // ignored
        }
    }

    public String takeFromReceivedCommands() throws InterruptedException {
        return receivedCommands.take();
    }

    public int getReceivedCommandsCount() {
        return receivedCommands.size();
    }

    public int getReceivedNonHeartbeatCommandsCount() {
        return receivedNonHeartbeatCommands.size();
    }

    public CommandExpectationFactory expect() {
        expectations = new LinkedList<>();
        return new CommandExpectationFactory(electionId, expectations);
    }


}
