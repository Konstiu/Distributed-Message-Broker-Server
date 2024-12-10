package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class BrokerCommandHandler implements Runnable {
    private final Socket socket;
    private OutputStream out;
    private BufferedReader in;
    private volatile ConcurrentHashMap<String, Exchange> exchanges;
    private volatile List<Queue> queues;
    private volatile AtomicReference<String> bind;
    private volatile AtomicReference<Exchange> lastExchange;
    private final BrokerConfig config;

    public BrokerCommandHandler(Socket socket, ConcurrentHashMap<String, Exchange> exchanges, List<Queue> queues, AtomicReference<String> bind, AtomicReference<Exchange> exchange, BrokerConfig config) {
        this.socket = socket;
        this.exchanges = exchanges;
        this.queues = queues;
        this.bind = bind;
        this.lastExchange = exchange;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            out = socket.getOutputStream();
            out.write("ok SMQP\n".getBytes());
            out.flush();

            String line;
            String currenQueue = null;
            Exchange currentExchange = null;
            this.exchanges.put("default", new Exchange(ExchangeType.DEFAULT, this.queues, new ConcurrentHashMap<>()));

            while (((line = in.readLine()) != null)) {
                if (line.equals("stop") || socket.isClosed()) {
                    if (currentExchange != null) {
                        currentExchange.unsubscribe(this);
                    } else {
                        if (lastExchange != null) {
                            lastExchange.get().unsubscribe(this);
                        }
                    }
                }
                String[] command = line.split(" ");
                switch (command[0]) {
                    case "exchange":
                        currentExchange = exchangeType(command, exchanges, out);
                        lastExchange.set(currentExchange);
                        break;
                    case "queue":
                        currenQueue = queue(command, lastExchange.get(), out);
                        break;
                    case "bind":
                        bind.set(setBindings(command, lastExchange.get(), currenQueue, out));
                        break;
                    case "subscribe":
                        subscribe(command, lastExchange.get(), currenQueue, bind.get());
                        break;
                    case "publish":
                        publish(command, currentExchange);
                        break;
                    case "exit":
                        out.write("ok bye\n".getBytes());
                        out.flush();
                        Thread.currentThread().interrupt();
                        break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void publish(String[] command, Exchange currentExchange) throws IOException {
        if (command.length != 3) {
            out.write("error\n".getBytes());
            out.flush();
            return;
        }
        if (currentExchange != null) {
            out.write("ok\n".getBytes());
            out.flush();
            String msg = String.join(" ", Arrays.copyOfRange(command, 2, command.length));
            currentExchange.publish(command[1], msg);
            sendMsgToMonitoringServer(command[1]);
        } else {
            out.write("error no exchange declared\n".getBytes());
            out.flush();
        }
    }

    private void subscribe(String[] command, Exchange currentExchange, String currenQueue, String bind) throws IOException {
        if (command.length != 1) {
            out.write("error\n".getBytes());
            out.flush();
            return;
        }
        if (currenQueue == null) {
            out.write("error no queue declared\n".getBytes());
            out.flush();
            return;
        }
        if (Objects.equals(bind, "") && !currentExchange.getType().equals(ExchangeType.DEFAULT)) {
            out.write("error no bind declared\n".getBytes());
            out.flush();
            return;
        }
        out.write("ok\n".getBytes());
        out.flush();
        currentExchange.subscribed(this, currenQueue);
    }

    private Exchange exchangeType(String[] command, ConcurrentHashMap<String, Exchange> exchanges, OutputStream out) throws Exception {
        if (command.length != 3) {
            out.write("error\n".getBytes());
            out.flush();
            return null;
        }
        ExchangeType e =
                command[1].equals("topic") ? ExchangeType.TOPIC :
                        command[1].equals("direct") ? ExchangeType.DIRECT :
                                command[1].equals("fanout") ? ExchangeType.FANOUT :
                                        command[1].equals("default") ? ExchangeType.DEFAULT : null;
        if (e == null) {
            out.write("error\n".getBytes());
            out.flush();
            return null;
        }

        if (exchanges.containsKey(command[2])) {
            if (!exchanges.get(command[2]).getType().equals(e)) {
                out.write("error exchange already exists with different type\n".getBytes());
                out.flush();
                return null;
            }
            out.write("ok\n".getBytes());
            out.flush();
            return exchanges.get(command[2]);
        }

        exchanges.computeIfAbsent(command[2], k -> new Exchange(e, queues, new ConcurrentHashMap<>()));
        //Exchange exchange = new Exchange(e, queues, new ConcurrentHashMap<>());
        //exchanges.put(command[2], exchange);
        out.write("ok\n".getBytes());
        out.flush();
        return exchanges.get(command[2]);
    }

    private String queue(String[] command, Exchange currentExchange, OutputStream out) throws Exception {
        if (command.length != 2) {
            out.write("error\n".getBytes());
            out.flush();
            return null;
        }

        Queue a = new Queue(command[1], this, "");
        exchanges.get("default").addQueue(a);
        exchanges.get("default").addBinding(command[1], command[1]);
        out.write("ok\n".getBytes());
        out.flush();
        return command[1];
    }

    private String setBindings(String[] command, Exchange currentExchange, String currenQueue, OutputStream out) throws Exception {
        if (command.length != 2) {
            out.write("error\n".getBytes());
            out.flush();
            return bind.get();
        }
        if (currentExchange == null) {
            out.write("error no exchange declared\n".getBytes());
            out.flush();
            return bind.get();
        }
        if (currenQueue == null) {
            out.write("error no queue declared\n".getBytes());
            out.flush();
            return bind.get();
        }
        currentExchange.addBinding(currenQueue, command[1]);
        out.write("ok\n".getBytes());
        out.flush();
        return command[1];
    }

    public void printmsg(String msg) {
        try {
            out.write((msg).getBytes());
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMsgToMonitoringServer(String key) {
        try (DatagramSocket socket = new DatagramSocket()) {
            String msg = config.host() + ":" + config.port() + " " + key;
            byte[] data = msg.getBytes();
            InetAddress address = InetAddress.getByName(config.monitoringHost());
            DatagramPacket packet = new DatagramPacket(data, data.length, address, config.monitoringPort());
            socket.send(packet);
        } catch (Exception e) {
            if (config.monitoringPort() != 0) {
                throw new RuntimeException(e);
            }
        }

    }
}
