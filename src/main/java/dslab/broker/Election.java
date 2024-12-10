package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Election implements Runnable {
    private BrokerConfig config;
    private BufferedReader in;
    private OutputStream out;
    private List<Socket> socketList;
    private Socket nextSocket;
    private ExecutorService executor;
    private boolean running = true;
    private ElectionHandler election;

    private Thread a;

    private int leaderId;

    private ServerSocket serverSocket;
    public Election(BrokerConfig config) {
        this.config = config;
        socketList = new ArrayList<>();
        executor = Executors.newVirtualThreadPerTaskExecutor();

    }

    @Override
    public void run() {
        if (config.electionType().equals("none")) {
            return;
        }

        try {
            this.serverSocket = new ServerSocket(config.electionPort());
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout((int) config.electionHeartbeatTimeoutMs());
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    OutputStream out = socket.getOutputStream();
                    write(out, "ok LEP\n");
                    String message = in.readLine();
                    if (message == null) {
                        throw new IOException("Connection closed");
                    }
                    if (!message.equals("ok")) {
                        throw new IOException("Expected 'ok' but got '" + message + "'");
                    }

                    election = new ElectionHandler(config, this, socket);
                    executor.submit(election);
                } catch (IOException e) {

                    switch (config.electionType()) {
                        case "ring":
                            ringElection(-1);
                            break;
                        case "bully":
                            System.out.println("Bully election");
                            break;
                        case "flooding":
                            System.out.println("Flooding election");
                            break;
                        default:
                            throw new RuntimeException("Unknown election type: " + config.electionType());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up server socket", e);
        }
    }


    protected void ringElection(int id) {
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

    protected void declare(int id) {
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

    protected void ping() {
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

    private void write(OutputStream out, String message) {
        try {
            out.write(message.getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getLeader() {
        return this.leaderId;
    }
    public void shutdown() {
        try {
            running = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                this.serverSocket.close();
            }
            if (nextSocket != null && !nextSocket.isClosed())
                nextSocket.close();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


