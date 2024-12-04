package dslab.dns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class DNSCommandHandler implements Runnable {
    private final Socket socket;
    private final ConcurrentHashMap<String, String> dnsRecords;
    private final OutputStream outputStream;

    public DNSCommandHandler(Socket socket, ConcurrentHashMap<String, String> dnsRecords) {
        this.socket = socket;
        this.dnsRecords = dnsRecords;
        try {
            outputStream = this.socket.getOutputStream();
            outputStream.write("ok SDP\n".getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String command;
            while ((command = in.readLine()) != null) {
                String[] arr = command.split(" ");
                if (arr.length == 0) {
                    arr = new String[]{command};
                }
                switch (arr[0]) {
                    case "register":
                        if (arr.length != 3) {
                            outputStream.write("error usage: register <name> <ip:port>\n".getBytes());
                            outputStream.flush();
                            break;
                        }
                        dnsRecords.put(arr[1], arr[2]);
                        outputStream.write("ok\n".getBytes());
                        outputStream.flush();
                        break;
                    case "unregister":
                        if (arr.length != 2) {
                            outputStream.write("error usage: unregister <name>\n".getBytes());
                            outputStream.flush();
                            break;
                        }
                        dnsRecords.remove(arr[1]);
                        outputStream.write("ok\n".getBytes());
                        outputStream.flush();
                        break;
                    case "resolve":
                        if (arr.length != 2){
                            outputStream.write("error usage: resolve <name>\n".getBytes());
                            outputStream.flush();
                            break;
                        }
                        if (!dnsRecords.containsKey(arr[1])) {
                            outputStream.write("error domain\n".getBytes());
                            outputStream.flush();
                            break;
                        }
                        outputStream.write((dnsRecords.get(arr[1]) + "\n").getBytes());
                        break;
                    case "exit":
                        outputStream.write("ok bye\n".getBytes());
                        outputStream.flush();
                        outputStream.close();
                        socket.close();
                        Thread.currentThread().interrupt();
                        break;
                    default:
                        outputStream.write("error unknown command\n".getBytes());
                        outputStream.flush();
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        try {
            if (socket.isClosed()) {
                return;
            }
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
