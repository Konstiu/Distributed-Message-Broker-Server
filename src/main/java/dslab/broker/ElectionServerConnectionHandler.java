package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ElectionServerConnectionHandler implements Runnable {
    private final Socket socket;
    private final BrokerConfig config;
    private AtomicInteger leader;
    private ExecutorService executorService;
    private volatile boolean running = true;

    public ElectionServerConnectionHandler(Socket socket, BrokerConfig config, AtomicInteger leader) {
        this.socket = socket;
        this.config = config;
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.leader = leader;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            OutputStream out = socket.getOutputStream();

            out.write("ok LEP\n".getBytes());
            out.flush();

            String line = in.readLine();
            System.out.println(line);
            if (line == null) {
                socket.close();
                return;
            }
            String[] parts = line.split(" ");
            switch (parts[0]) {
                case "elect":
                    out.write("ok\n".getBytes());
                    out.flush();
                    handleElection(Integer.parseInt(parts[1]));
                    break;
                case "declare":
                    out.write(("ack " + config.electionId() + "\n").getBytes());
                    out.flush();
                    handleDeclare(Integer.parseInt(parts[1]));
                    break;
                case "ping":
                    out.write("pong\n".getBytes());
                    out.flush();
                    Thread.sleep(50);
                    sendPing();
                    break;
            }

        } catch (Exception e) {
            if (running) {
                System.out.println("Error1");
                throw new RuntimeException(e);
            }
        }
    }

    private void sendPing() throws IOException {
        Socket socket = new Socket(config.electionPeerHosts()[0], config.electionPeerPorts()[0]);
        OutputStream out = socket.getOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String message = in.readLine();
        if (message == null || !message.equals("ok LEP")) {
            socket.close();
            System.out.println("Error");
            return;
        }
        out.write(("ping\n").getBytes());
        out.flush();
        message = in.readLine();
        if (message == null || !message.equals("pong")) {
            System.out.println("Error");
            socket.close();
            return;
        }
        socket.close();
    }


    private void handleElection(int id) throws IOException {
        if (id == this.config.electionId()) {
            this.leader.set(id);
            if (sendElectionMessage("declare " + id)){
                System.out.println("Error");
            }

        } else if (id > this.config.electionId()) {
            if (sendElectionMessage("elect " + id)){
                System.out.println("Error");
            }
        } else {
            if (sendElectionMessage("elect " + config.electionId())){
                System.out.println("Error");
            }
        }
    }

    private boolean sendElectionMessage(String command) throws IOException {
        Socket socket = null;
        try {
            for (int i = 0; i < config.electionPeerIds().length; i++) {
                try {
                    socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
                    BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                    OutputStream out = socket.getOutputStream();
                    break;
                } catch (IOException ignored) {
                    continue;
                }
            }
            if (socket == null) {
                return true;
            }
            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String message = in.readLine();
            if (message == null || !message.equals("ok LEP")) {
                return true;
            }
            out.write((command + "\n").getBytes());
            out.flush();

            message = in.readLine();
            return message == null || (command.startsWith("declare") ? !message.split(" ")[0].equals("ack") : !message.equals("ok"));
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }


    private void handleDeclare(int id) throws IOException {
        this.leader.set(id);

        if (id == this.config.electionId()) {
            connectToDNS();
            executorService.submit(new ElectionHeartbeat(socket, this.socket, Thread.currentThread()));
        } else {
            if (sendElectionMessage("declare " + id))
                System.out.println("Error");
        }
    }

    private void connectToDNS() {
        try {
            Socket socket = new Socket(config.dnsHost(), config.dnsPort());
            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (!in.readLine().equals("ok SDP")) {
                out.write("Error connecting to DNS Server".getBytes());
                out.flush();
            }

            out.write(("register " + config.electionDomain() + " " + config.host() + ":" + config.port() + "\n").getBytes());
            out.flush();
            if (!in.readLine().equals("ok")) {
                out.write("Error registering with DNS Server".getBytes());
                out.flush();
            }
            out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void shutdown() {
        running = false;
        executorService.shutdown();
    }

}
