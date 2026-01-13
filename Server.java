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

    // ===== SERVER CONSOLE =====
    private void handleServerCommands() {
        Scanner sc = new Scanner(System.in);
//        chế độ sever
        String mode = null;
//        Lưu tên client đang chat riêng
        String targetUser = null;

        while (true) {
            if (mode == null) System.out.print("SERVER> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            if (mode != null) {
                if (line.equalsIgnoreCase("/close")) {
                    System.out.println("Thoát chế độ " + mode);
                    mode = null;
                    targetUser = null;
                    continue;
                }
                switch (mode) {
                    case "/broadcast":
//                        null là broadcasr sever
                        broadcastMessage("[SERVER_BROADCAST] " + line, null);
                        break;
                    case "/msg":
                        if (targetUser != null)
                            sendPrivateMessage("SERVER", targetUser, line);
                        break;
                }
                continue;
            }

            String[] parts = line.split(" ", 2);
//            Lưu lệnh chính mà server nhập vào, ví dụ /msg, /broadcast, /kick, /exit
            String command = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "/menu":
                    printMenu();
                    break;
                case "/online":
                    listClients(System.out);
                    break;
                case "/exit":
                    System.out.println("Đóng server...");
                    shutdownServer();
                    return;
                case "/broadcast":
                    mode = "/broadcast";
                    System.out.println("Bắt đầu broadcast (gõ /close để thoát)");
                    break;
                case "/msg":
                    if (!args.isEmpty()) {
//                        gắn tên nhập lần đầu
                        targetUser = args;
                        if (!clients.containsKey(targetUser)) {
                            System.out.println("Không tìm thấy client: " + targetUser);
                            targetUser = null;
                        } else {
                            mode = "/msg";
                            System.out.println("Bắt đầu chat liên tục với " + targetUser + " (gõ /close để thoát)");
                        }
                    } else System.out.println("Cú pháp: /msg <tên_user>");
                    break;
                case "/kick":
                    if (!args.isEmpty()) kickUser(args);
                    else System.out.println("Cú pháp: /kick <tên_user>");
                    break;
                default:
                    System.out.println("Lệnh không hợp lệ! Gõ /menu để xem các lệnh.");
                    break;
            }
        }
    }

    private void printMenu() {
        System.out.println("\n================== SERVER MENU ==================");
        System.out.println("/online      ➜ Xem danh sách clients online (tên + IP)");
        System.out.println("/msg <user>  ➜ Chat riêng liên tục với client (gõ /close để thoát)");
        System.out.println("/broadcast   ➜ Gửi broadcast tới tất cả client");
        System.out.println("/kick <user> ➜ Kick client khỏi server");
        System.out.println("/exit        ➜ Thoát server");
        System.out.println("/menu        ➜ Mở menu");
        System.out.println("=================================================");
    }
}
    public void broadcastMessage(String message, String senderUsername) {
        saveMessageToHistory(message);
        System.out.println("[BROADCAST] " + message);
        for (ClientHandler client : clients.values()) {
            String currentUsername = client.getUsername();
            if (currentUsername == null) continue;
            if (senderUsername == null || !currentUsername.equals(senderUsername))
                client.sendMessage(message);
        }
    }