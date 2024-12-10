package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

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
        socket.setSoTimeout((int) config.electionHeartbeatTimeoutMs());
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
                String[] parts = message.split(" ");
                switch (parts[0]) {
                    case "ping":
                        //System.out.println("ping");
                        write(out, "pong\n");
                        election.ping();
                        break;
                    case "elect":
                        int id = Integer.parseInt(parts[1]);
                        //System.out.println("Received election message from " + id);
                        election.ringElection(id);
                        break;
                    case "declare":
                        int elected = Integer.parseInt(parts[1]);
                        //System.out.println("Received declare message from " + elected);
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
            System.out.println("write error");
            throw new RuntimeException(e);
        }
    }

    private void ringElection(int id) {
        if (nextSocket == null) {
            try {
                nextSocket = new Socket(config.electionPeerHosts()[0], config.electionPeerPorts()[0]);
                BufferedReader in = new BufferedReader(new InputStreamReader(nextSocket.getInputStream()));
                if (in.readLine().equals("ok LEP")) {
                    write(nextSocket.getOutputStream(), "ok\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (id > config.electionId()) {
            try {
                //Socket socket = new Socket(config.electionPeerHosts()[0], config.electionPeerPorts()[0]);
                write(nextSocket.getOutputStream(), "elect " + id + "\n");
            } catch (IOException e) {
                System.out.println("Error sending election message");
                throw new RuntimeException(e);
            }
        }
        if (id == config.electionId()) {
            try {
                //Socket socket = new Socket(config.electionPeerHosts()[0], config.electionPeerPorts()[0]);
                write(nextSocket.getOutputStream(), "declare " + id + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (id < config.electionId()) {
            try {
                //Socket socket = new Socket(config.electionPeerHosts()[0], config.electionPeerPorts()[0]);
                write(nextSocket.getOutputStream(), "elect " + config.electionId() + "\n");
            } catch (IOException e) {
                if (running)
                    throw new RuntimeException(e);
            }
        }
    }

    private void declare(int id) {
        leaderId = id;
        if (id == config.electionId()) {
            try {
                Socket socket = new Socket(config.dnsHost(), config.dnsPort());
                OutputStream out = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                if (!reader.readLine().equals("ok SDP")) {
                    out.write("Error connecting to DNS Server".getBytes());
                    out.flush();
                }
                out.write(("register " + config.electionDomain() + " " + config.host() + ":" + config.port() + "\n").getBytes());
                out.flush();
                if (!reader.readLine().equals("ok")) {
                    out.write("Error registering with DNS Server".getBytes());
                    out.flush();
                }
                out.close();
                reader.close();
                socket.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            ping();
        }
        try {
            //Socket socket = new Socket(config.electionPeerHosts()[0], config.electionPeerPorts()[0]);
            write(nextSocket.getOutputStream(), "declare " + id + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ping() {
        try {
            write(nextSocket.getOutputStream(), "ping\n");
            BufferedReader in = new BufferedReader(new InputStreamReader(nextSocket.getInputStream()));
            if (in.readLine().equals("pong")) {
                //System.out.println("ping-pong");
                Thread.sleep(50);
                return;
            }
            System.out.println("no pong");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
