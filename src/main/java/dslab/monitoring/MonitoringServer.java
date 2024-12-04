package dslab.monitoring;

import dslab.ComponentFactory;
import dslab.config.MonitoringServerConfig;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

public class MonitoringServer implements IMonitoringServer {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> statistics;
    private final int port;
    private DatagramSocket socket;

    public MonitoringServer(MonitoringServerConfig config) {
        this.statistics = new ConcurrentHashMap<>();
        port = config.monitoringPort();
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            while (!socket.isClosed()) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedData = new String(packet.getData(), 0, packet.getLength());
                String[] parts = receivedData.split(" ");
                if (parts.length != 2) {
                    continue;
                }
                statistics.computeIfAbsent(parts[0], k -> new ConcurrentHashMap<>(1)).merge(parts[1], 1, Integer::sum);
            }
        } catch (Exception e) {
            if (!socket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {
        socket.close();
    }

    @Override
    public int receivedMessages() {
        return statistics.values().stream().mapToInt(m -> m.values().stream().mapToInt(Integer::intValue).sum()).sum();
    }

    @Override
    public String getStatistics() {
        return statistics.entrySet().stream()
                .map(e -> "Server " + e.getKey() + "\n" + e.getValue().entrySet().stream()
                        .map(e2 -> e2.getKey() + " " + e2.getValue())
                        .reduce((s1, s2) -> s1 + "\n" + s2).orElse(""))
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse("");
    }

    public static void main(String[] args) {
        ComponentFactory.createMonitoringServer(args[0]).run();
    }
}
