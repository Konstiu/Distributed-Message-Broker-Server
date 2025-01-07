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

    public ElectionHeartbeat(Socket socket1, Socket socket2) throws IOException {
        this.socket2 = socket2;
        this.socket1 = socket1;
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
                Thread.sleep(40);
            }
        } catch (Exception ignored) {
            try {
                socket2.close();
                socket1.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void shutdown(){
        try {
            socket1.close();
            socket2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
