package dslab.broker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

public class ElectionHeartbeat implements Runnable {
    private final Socket socket1;
    private final Socket socket2;
    private final Thread Election;

    public ElectionHeartbeat(Socket socket1, Socket socket2, Thread Election) throws IOException {
        this.socket2 = socket2;
        this.socket1 = socket1;
        this.Election = Election;
    }

    @Override
    public void run() {
        try {
            String message = null;

            BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
            OutputStream out1 = socket1.getOutputStream();

            BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
            OutputStream out2 = socket2.getOutputStream();


            while (true) {
                out1.write("ping\n".getBytes());
                out1.flush();
                message = in1.readLine();
                if (!Objects.equals(message, "pong")){
                    System.out.println(message);
                    System.out.println("no pong");
                }
                if (in2.readLine().equals("ping")){
                    out2.write("pong\n".getBytes());
                    out2.flush();
                }
                Thread.sleep(500);
            }
        } catch (Exception ignored) {}
    }
}
