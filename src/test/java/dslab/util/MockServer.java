package dslab.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static dslab.util.CommandBuilder.ack;
import static dslab.util.CommandBuilder.declare;
import static dslab.util.CommandBuilder.elect;
import static dslab.util.CommandBuilder.ok;
import static dslab.util.CommandBuilder.ping;
import static dslab.util.CommandBuilder.pong;
import static dslab.util.CommandBuilder.vote;

/**
 * Only supports a single connection
 */
public class MockServer implements Runnable {

    private static final String ERROR_RESPONSE = "INVALID MESSAGE RECEIVED";
    private static final String DEFAULT_EXPECTED_MESSAGE = "EXPECTED MESSAGE NOT SET, PLEASE SET A EXPECTED MESSAGE";
    private static final String DEFAULT_RESPONSE = "EXPECTED MESSAGE NOT SET, PLEASE SET A EXPECTED MESSAGE";

    private ServerSocket socket;
    private final int electionId;
    private final int port;
    private final BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> receivedNonPingMessages = new LinkedBlockingQueue<>();

    private String expectedMessage = DEFAULT_EXPECTED_MESSAGE;
    private String response = DEFAULT_RESPONSE;

    public MockServer(int port, int electionId) {
        this.port = port;
        this.electionId = electionId;
    }

    @Override
    public void run() {
        try (ServerSocket socket = new ServerSocket(port)) {
            this.socket = socket;

            while (!socket.isClosed()) {
                Socket conn = socket.accept();
                handleConnection(conn);
            }
        } catch (IOException e) {
            // ignored
        }
    }

    private void handleConnection(Socket conn) {
        try (
                conn;
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                PrintStream out = new PrintStream(conn.getOutputStream(), true)
        ) {

            out.println("ok LEP");
            boolean ok = true;
            while (!conn.isClosed() && ok) {
                ok = handleInput(in, out);
            }

        } catch (IOException e) {
            // ignored
        }
    }

    private boolean handleInput(BufferedReader in, PrintStream out) throws IOException {
        String read = in.readLine();

        if (read == null || read.isBlank()) return false;

        receivedMessages.add(read);

        if (!read.equals(ping()) || !read.equals(pong())) receivedNonPingMessages.add(read);

        if (ping().equals(read)) {
            out.println(pong());
            return true;
        }

        if (expectedMessage.equals(DEFAULT_EXPECTED_MESSAGE)) {
            out.println(DEFAULT_RESPONSE);
            return false;
        }

        if (!read.equals(expectedMessage)) {
            out.println(ERROR_RESPONSE);
            return false;
        }

        out.println(response);
        return true;
    }

    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // ignored
        }
    }

    public String takeMessage() throws InterruptedException {
        return receivedMessages.take();
    }

    public String getQueue() {
        return receivedMessages.toString();
    }

    public int receivedMessagesSize() {
        return receivedMessages.size();
    }

    public int receivedNonPingMessagesSize() {
        return receivedNonPingMessages.size();
    }

    public void expectElect(int id) {
        this.expectedMessage = elect(id);
        this.response = ok();
    }

    public void expectElect(int electId, int candidateId) {
        this.expectedMessage = elect(electId);
        this.response = vote(electionId, candidateId);
    }

    public void expectDeclare(int id) {
        this.expectedMessage = declare(id);
        this.response = ack(electionId);
    }

    public void expectPing() {
        this.expectedMessage = "ping";
        this.response = "pong";
    }

    public void setExpectationAndResponse(String expectedMessage, String response) {
        this.expectedMessage = expectedMessage;
        this.response = response;
    }
}
