package dslab.broker;

import dslab.ComponentFactory;
import dslab.config.BrokerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Broker implements IBroker {
    private final BrokerConfig config;
    private Thread dnsClient;
    private ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private List<Socket> sockets = new ArrayList<>();
    private ElectionServer election;

    public Broker(BrokerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            this.election = new ElectionServer(config);
            Thread.startVirtualThread(this.election);

            dnsClient = Thread.startVirtualThread(new DNSClient(config));
            executor = Executors.newVirtualThreadPerTaskExecutor();


            AtomicReference<String> bind = new AtomicReference<>("");
            AtomicReference<Exchange> test = new AtomicReference<>();

            ConcurrentHashMap<String, Exchange> exchanges = new ConcurrentHashMap<>();
            List<Queue> queues = new ArrayList<>();
            serverSocket = new ServerSocket(config.port());
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                sockets.add(socket);
                executor.submit(new BrokerCommandHandler(socket, exchanges, queues, bind, test, config));
            }

        } catch (Exception e) {
            if (running) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public int getId() {
        return config.electionId();
    }

    @Override
    public void initiateElection() {

    }

    @Override
    public int getLeader() {
        return this.election.getLeader();
    }

    @Override
    public void shutdown() {
        running = false;
        try {
            serverSocket.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
        dnsClient.interrupt();
        for (Socket socket : sockets) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.election.shutdown();
    }

    public static void main(String[] args) {
        ComponentFactory.createBroker(args[0]).run();
    }
}
