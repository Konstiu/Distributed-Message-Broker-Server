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
                    break;
                } catch (IOException ignored) {}
            }


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

    public void initiateElection(){
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
                break;
            } catch (IOException ignored) {}
        }
    }

    public void shutdown() {
        try {
            if (serverSocket!= null)
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
