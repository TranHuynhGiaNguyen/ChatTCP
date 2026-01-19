package chattcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 1436;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean isClosing = false;

    public Client() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Đã kết nối đến server: " + SERVER_IP + ":" + SERVER_PORT);

        } catch (Exception e) {
            System.err.println("Lỗi kết nối: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}
