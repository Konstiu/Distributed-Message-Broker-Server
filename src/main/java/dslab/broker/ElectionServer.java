package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ElectionServer implements Runnable {
    private final BrokerConfig config;
    private ExecutorService executor;
    private ServerSocket serverSocket;
    private List<ElectionServerConnectionHandler> handlers = new ArrayList<>();
    private final AtomicInteger leaderId = new AtomicInteger(-1);


    public ElectionServer(BrokerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        System.out.println("start");
        if (this.config.electionType() == "none") {
            return;
        }

        try {
            this.serverSocket = new ServerSocket(config.electionPort());
            this.serverSocket.setSoTimeout((int) config.electionHeartbeatTimeoutMs());
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            while (!this.serverSocket.isClosed()) {
                ElectionServerConnectionHandler handler = new ElectionServerConnectionHandler(serverSocket.accept(), config, leaderId);
                handlers.add(handler);
                executor.submit(handler);
            }
        } catch (SocketTimeoutException e) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            restartServer();

            Socket socket = null;
            boolean isLeader = true;
            for (int i = 0; i < config.electionPeerIds().length; i++) {
                if (Objects.equals(this.config.electionType(), "bully") && this.config.electionId() > config.electionPeerIds()[i]) {
                    continue;
                }
                try {
                    socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
                    BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                    OutputStream out = socket.getOutputStream();
                    String message = in.readLine();
                    if (message == null || !message.equals("ok LEP")) {
                        socket.close();
                        System.out.println("Error");
                        return;
                    }
                    out.write(("elect " + config.electionId() + "\n").getBytes());
                    out.flush();
                    in.readLine();
                    socket.close();
                    if (Objects.equals(this.config.electionType(), "bully")) {
                        isLeader = false;
                        continue;
                    }
                    break;
                } catch (IOException ignored) {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }

            if (isLeader && Objects.equals(this.config.electionType(), "bully")) {
                leaderId.set(config.electionId());
                try {
                    socket = new Socket(config.dnsHost(), config.dnsPort());
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
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                for (int i = 0; i < config.electionPeerIds().length; i++) {
                    try {
                        socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
                        BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                        OutputStream out = socket.getOutputStream();
                        String message = in.readLine();
                        if (message == null || !message.equals("ok LEP")) {
                            socket.close();
                            System.out.println("Error");
                            return;
                        }
                        out.write(("declare " + config.electionId() + "\n").getBytes());
                        out.flush();
                        socket.close();
                    } catch (IOException ignored) {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                }
                for (ElectionServerConnectionHandler handler : handlers) {
                    try {handler.sendOnlyPing();} catch (Exception ignored) {}
                }
            }

            System.out.println("finish");


        } catch (IOException ignored) {
            if (serverSocket.isClosed()) {
                return;
            }
            System.out.println("other");
        }
    }

    private void restartServer() {

        executor.submit(() -> {
            try {
                this.serverSocket = new ServerSocket(config.electionPort());
                while (!this.serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ElectionServerConnectionHandler handler = new ElectionServerConnectionHandler(clientSocket, config, leaderId);
                    handlers.add(handler);
                    executor.submit(handler);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public int getLeader() {
        return this.leaderId.get();
    }

    public void initiateElection() {
        if (Objects.equals(this.config.electionType(), "bully")) {
            boolean isLeader = true;
            for (int i = 0; i < config.electionPeerIds().length; i++) {
                if (this.config.electionId() < config.electionPeerIds()[i]) {
                    try {
                        Socket socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
                        BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                        OutputStream out = socket.getOutputStream();
                        String message = in.readLine();
                        if (message == null || !message.equals("ok LEP")) {
                            socket.close();
                            System.out.println("Error");
                            return;
                        }
                        out.write(("elect " + config.electionId() + "\n").getBytes());
                        out.flush();
                        socket.close();
                        isLeader = false;
                    } catch (IOException ignored) {
                    }
                }
            }
            if (isLeader) {
                for (int i = 0; i < config.electionPeerIds().length; i++) {
                    try {
                        Socket socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
                        BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                        OutputStream out = socket.getOutputStream();
                        String message = in.readLine();
                        if (message == null || !message.equals("ok LEP")) {
                            socket.close();
                            System.out.println("Error");
                            return;
                        }
                        out.write(("declare " + config.electionId() + "\n").getBytes());
                        out.flush();
                        socket.close();
                        leaderId.set(config.electionId());
                    } catch (IOException ignored) {
                    }
                }
                for (int i = 0; i < handlers.size(); i++) {
                    try {
                        handlers.get(i).sendOnlyPing();
                    } catch (Exception ignored) {
                    }
                }
            }
            return;
        }
        for (int i = 0; i < config.electionPeerIds().length; i++) {
            try {
                Socket socket = new Socket(config.electionPeerHosts()[i], config.electionPeerPorts()[i]);
                BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                OutputStream out = socket.getOutputStream();
                String message = in.readLine();
                if (message == null || !message.equals("ok LEP")) {
                    socket.close();
                    System.out.println("Error");
                    return;
                }
                out.write(("elect " + config.electionId() + "\n").getBytes());
                out.flush();
                socket.close();
                break;
            } catch (IOException ignored) {
            }
        }
    }

    public void shutdown() {
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (executor != null)
            executor.shutdown();
        for (ElectionServerConnectionHandler handler : handlers) {
            handler.shutdown();
        }
    }
}
