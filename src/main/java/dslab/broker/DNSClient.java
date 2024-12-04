package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.Socket;


public class DNSClient implements Runnable {
    private final BrokerConfig config;

    public DNSClient(BrokerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(config.dnsHost(), config.dnsPort());
            OutputStream out = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            if (!reader.readLine().equals("ok SDP")) {
                out.write("Error connecting to DNS Server".getBytes());
                out.flush();
            }
            out.write(("register " + config.domain() + " " + config.host() + ":" + config.port() + "\n").getBytes());
            out.flush();
            if (!reader.readLine().equals("ok")) {
                out.write("Error registering with DNS Server".getBytes());
                out.flush();
            }
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
    }
}
