package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ElectionHandler implements Runnable {
    private BufferedReader in;
    private OutputStream out;
    private Election election;
    private BrokerConfig config;
    private Socket s;
    private Socket nextSocket;
    private int leaderId;
    private boolean running = true;

    public ElectionHandler(BrokerConfig config, Election election, Socket socket) throws IOException {
//        socket.setSoTimeout((int) config.electionHeartbeatTimeoutMs());
        this.out = socket.getOutputStream();
        this.election = election;
        this.config = config;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        s = socket;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                String[] parts = message.split(" ");
                switch (parts[0]) {
                    case "ping":
                        write(out, "pong\n");
                        election.ping();
                        break;
                    case "elect":
                        System.out.println(message);
                        int id = Integer.parseInt(parts[1]);
                        election.ringElection(id);
                        break;
                    case "declare":
                        int elected = Integer.parseInt(parts[1]);
                        election.declare(elected);
                        break;
                    default:
                        throw new RuntimeException("Unknown message: " + message);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(OutputStream out, String message) {
        try {
            out.write(message.getBytes());
            out.flush();
        } catch (IOException e) {
//            System.err.println("write error");
            throw new RuntimeException(e);
        }
    }
}
