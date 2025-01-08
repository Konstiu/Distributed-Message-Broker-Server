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
    private ElectionHeartbeat electionHeartbeat;

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
            if (line == null) {
                socket.close();
                return;
            }
            String[] parts = line.split(" ");
            switch (parts[0]) {
                case "elect":
                    if (parts.length != 2) {
                        out.write("error usage: elect <id>\n".getBytes());
                        out.flush();
                        return;
                    }
                    out.write("ok\n".getBytes());
                    out.flush();
                    switch (this.config.electionType()) {
                        case "ring" -> handleElectionRing(Integer.parseInt(parts[1]));
                        case "bully" -> handleElectionBully(Integer.parseInt(parts[1]));
                        case "raft" -> handleElectionRing(Integer.parseInt(parts[1]));//handleElectionRaft(Integer.parseInt(parts[1]));
                    }
                    break;
                case "declare":
                    if (parts.length != 2) {
                        out.write("error usage: declare <id>\n".getBytes());
                        out.flush();
                        return;
                    }
                    leader.set(Integer.parseInt(parts[1]));
                    out.write(("ack " + config.electionId() + "\n").getBytes());
                    out.flush();
                    switch (this.config.electionType()) {
                        case "ring" -> handleDeclareRing(Integer.parseInt(parts[1]));
                        case "bully" -> handleDeclareBully(Integer.parseInt(parts[1]));
                        case "raft" -> handleDeclareRing(Integer.parseInt(parts[1]));//handleDeclareRaft(Integer.parseInt(parts[1]));
                    }
                    break;
                case "ping":
                    out.write("pong\n".getBytes());
                    out.flush();
                    break;
            }

        } catch (Exception e) {
            if (running) {
                System.out.println("Error1");
                throw new RuntimeException(e);
            }
        }
    }

    private void handleDeclareRaft(int id) {

    }

    private void handleDeclareBully(int id) {
        this.leader.set(id);
    }

    private void handleElectionRaft(int id) {

    }

    private void handleElectionBully(int id) {
        //System.out.println("Election Bully " + id);
        if (id > this.config.electionId()){
            return;
        }
        System.out.println("Election Bully " + id);
        sendToAllBully("elect " + config.electionId());
    }

    private void sendToAllBully(String command){
        boolean isLeader = true;
        for (int i = 0; i < config.electionPeerIds().length; i++) {
            if (config.electionId() > config.electionPeerIds()[i]) {
                continue;
            }
            if (connectToSocketAndSendCommand(command, i)){
                isLeader = false;
            }
        }
        if (isLeader){
            this.leader.set(config.electionId());
            for (int i = 0; i < config.electionPeerIds().length; i++) {
                connectToSocketAndSendCommand("declare " + config.electionId(), i);
            }
            try{
                sendOnlyPing();
                connectToDNS();
                electionHeartbeat = new ElectionHeartbeat(socket, this.socket);
                executorService.submit(electionHeartbeat);
            }catch (IOException e){
                System.out.println("Error");
            }
        }
    }

    private boolean connectToSocketAndSendCommand(String command, int i){
        Socket socket = null;
        try {
            socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
            BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream();
            String message = in.readLine();
            if (message == null || !message.equals("ok LEP")) {
                socket.close();
                System.out.println("Error");
                return false;
            }
            out.write((command + "\n").getBytes());
            out.flush();
            message = in.readLine();
            if (message == null || (!message.equals("ok") && !message.startsWith("ack") && !message.startsWith("vote"))) {
                socket.close();
                System.out.println("Error");
                return false;
            }
            socket.close();
            return true;
        } catch (IOException ignored) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored2) {
                }
            }
        }
        return false;
    }


    private void handleElectionRing(int id) throws IOException {
        if (id == this.config.electionId()) {
            this.leader.set(id);
            if (sendElectionMessage("declare " + id)) {
                System.out.println("Error");
            } else {
                sendOnlyPing();
            }

        } else if (id > this.config.electionId()) {
            if (sendElectionMessage("elect " + id)) {
                System.out.println("Error");
            }
        } else {
            if (sendElectionMessage("elect " + config.electionId())) {
                System.out.println("Error");
            }
        }
    }

    private boolean sendElectionMessage(String command) throws IOException {
        for (int i = 0; i < config.electionPeerIds().length; i++) {
            if (connectToSocketAndSendCommand(command, i)) {
                return false;
            }
        }
        return true;
    }


    private void handleDeclareRing(int id) throws IOException {
        this.leader.set(id);
        if (id == this.config.electionId()) {
            connectToDNS();
            electionHeartbeat = new ElectionHeartbeat(socket, this.socket);
            executorService.submit(electionHeartbeat);
        } else {
            if (sendElectionMessage("declare " + id))
                System.out.println("Error");
        }
    }

    private void connectToDNS() {
        try {
            Socket socket = new Socket(config.dnsHost(), config.dnsPort());
            OutputStream out = socket.getOutputStream();
            connectToDNS(socket, out, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void connectToDNS(Socket socket, OutputStream out, BrokerConfig config) throws IOException {
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
    }


    public void shutdown() {
        running = false;
        if (electionHeartbeat != null)
            electionHeartbeat.shutdown();
        executorService.shutdown();
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendOnlyPing() throws IOException {
        for (int i = 0; i < 2; i++){
            Socket socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
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
    }
}
