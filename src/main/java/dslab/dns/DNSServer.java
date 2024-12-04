package dslab.dns;

import dslab.ComponentFactory;
import dslab.config.DNSServerConfig;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DNSServer implements IDNSServer {
    private final DNSServerConfig config;
    private ServerSocket serverSocket;
    private ExecutorService executors;
    private final ConcurrentHashMap<String, String> dnsRecords = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private List<DNSCommandHandler> commandHandlers = new ArrayList<>();

    public DNSServer(DNSServerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(config.port());
            executors = Executors.newVirtualThreadPerTaskExecutor();
            while (!serverSocket.isClosed() && running) {
                Socket socket = serverSocket.accept();
                DNSCommandHandler dnsCommandHandler = new DNSCommandHandler(socket, dnsRecords);
                commandHandlers.add(dnsCommandHandler);
                executors.submit(dnsCommandHandler);
            }
        } catch (Exception e) {
            if (running) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            running = false;
            for (DNSCommandHandler commandHandler : commandHandlers) {
                commandHandler.shutdown();
            }

            if (executors != null && !executors.isShutdown()) {
                executors.shutdown();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        ComponentFactory.createDNSServer(args[0]).run();
    }
}
