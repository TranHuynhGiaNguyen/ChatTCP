package chattcp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final int SERVER_PORT = 1436;
    private ServerSocket serverSocket;
    //map tên user và ClientHandler của user đó
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    //    tên user và trạng thái của user
    private final Map<String, String> onlineInfo = new ConcurrentHashMap<>();
    //    lưu lịch sử chat
    private final List<String> chatHistory = Collections.synchronizedList(new ArrayList<>());

    public Server() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server đang chạy tại cổng " + SERVER_PORT);
            printMenu();

            // Luồng nhận client mới
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket, this);
                        new Thread(handler).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) e.printStackTrace();
                    }
                }
            }).start();

            handleServerCommands();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleServerCommands() {
        Scanner sc = new Scanner(System.in);
        String mode = null;
        String targetUser = null;
        while (true) {
            if (mode == null) System.out.print("SERVER> ");
            String line = sc.nextLine().trim();
            // ... (logic xử lý command exit, menu, mode)
        }
    }

    private void printMenu() {
        System.out.println("\n================== SERVER MENU ==================");
        System.out.println("/online      ➜ Xem danh sách clients online (tên + IP)");
        System.out.println("/msg <user>  ➜ Chat riêng liên tục với client");
        System.out.println("/broadcast   ➜ Gửi broadcast tới tất cả client");
        // ...
    }