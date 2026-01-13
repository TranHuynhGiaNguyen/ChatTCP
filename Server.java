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