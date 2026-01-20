package chattcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

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
            Scanner sc = new Scanner(System.in);

            System.out.println("Đã kết nối đến server: " + SERVER_IP + ":" + SERVER_PORT);

            // --- Luồng nhận tin nhắn ---
            Thread readThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = reader.readLine()) != null) {
                        if (serverMsg.equals("[SERVER_KICK]")) {
                            System.out.println("Bạn đã bị kick khỏi server!");
                            closeClient();
                            break;
                        }
                        if (serverMsg.equals("[SERVER] Server đóng. Đang thoát...")) {
                            System.out.println("Server đã đóng, mọi người thoát khỏi phòng chat");
                            closeClient();
                            break;
                        }
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    // Chỉ báo lỗi nếu không phải do người dùng chủ động thoát
                    if (!isClosing) {
                        System.out.println("Server đã ngắt kết nối.");
                        closeClient();
                    }
                }
            });

        } catch (IOException e) {
            System.err.println("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void closeClient() {
        if (isClosing) return;
        isClosing = true;

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }

        System.out.println("Đã thoát chương trình.");
        System.exit(0);
    }

    public static void main(String[] args) {
        new chattcp.Client();
    }
}