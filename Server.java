package chattcp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private final int SERVER_PORT = 1436;
    private ServerSocket serverSocket;

    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, String> onlineInfo = new ConcurrentHashMap<>();
    private final List<String> chatHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Message> messageStore = new ConcurrentHashMap<>();

    private ServerGUI gui;

    public Server(ServerGUI gui) {
        this.gui = gui;
        startServer();
    }

    public Server() {
        this.gui = null;
        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            log("🚀 Server đang chạy tại cổng " + SERVER_PORT);

            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket, this);
                        new Thread(handler).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed())
                            log("❌ Lỗi accept: " + e.getMessage());
                    }
                }
            }, "AcceptThread").start();

        } catch (IOException e) {
            log("❌ Không thể khởi động server: " + e.getMessage());
        }
    }

    public void log(String msg) {
        System.out.println(msg);
        if (gui != null) gui.appendLog(msg);
    }

    public void listClientNames(PrintWriter out) {
        if (clients.isEmpty()) {
            out.println("[SERVER] Chưa có client nào online.");
            return;
        }

        out.println("=== 👥 Clients online ===");
        for (String username : clients.keySet()) {
            out.println(" - " + username);
        }
    }

    public List<String> getOnlineNames() {
        return new ArrayList<>(clients.keySet());
    }

    public void updateGUIOnline() {
        if (gui != null) gui.updateOnlineList(getOnlineNames());
    }

    public boolean registerClient(String username, ClientHandler handler) {
        Socket sock = handler.getSocket();
        String ip = sock != null ? sock.getRemoteSocketAddress().toString() : "Unknown";

        synchronized (clients) {
            if (clients.containsKey(username)) return false;
            clients.put(username, handler);
            onlineInfo.put(username, ip);
        }

        broadcastMessage("📢 " + username + " đã tham gia phòng.", null);
        updateGUIOnline();
        log("✅ " + username + " đã kết nối từ " + ip);

        return true;
    }

    public void removeClient(String username) {
        clients.remove(username);
        onlineInfo.remove(username);

        broadcastMessage("📢 " + username + " đã rời phòng.", null);
        updateGUIOnline();
        log("👋 " + username + " đã ngắt kết nối");
    }

    public User getUser(String username) {
        ClientHandler handler = clients.get(username);
        return handler != null ? handler.user : null;
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public void kickUser(String username) {
        ClientHandler c = clients.get(username);
        if (c != null) {
            c.sendMessage("[SERVER] Bạn đã bị kick!");
            c.kick();
            log("🚫 Đã kick user: " + username);
        } else {
            log("❌ Không tìm thấy user: " + username);
        }
    }

    public void broadcastMessage(String message, String excludeUser) {
        saveMessage(message);
        // Only log user messages, not system notifications
        if (!message.startsWith("📢")) {
            log("[CHAT] " + message);
        }

        for (ClientHandler c : clients.values()) {
            String u = c.getUsername();
            if (u == null) continue;
            if (excludeUser == null || !u.equals(excludeUser))
                c.sendMessage(message);
        }
    }

    public void sendPrivateMessage(String fromUser, String toUser, String message) {
        ClientHandler receiver = clients.get(toUser);
        ClientHandler sender = clients.get(fromUser);

        if (receiver != null) {
            receiver.sendMessage("[" + fromUser + "] ➜ " + message);
        }

        if (sender != null && !fromUser.equalsIgnoreCase("SERVER")) {
            sender.sendMessage("[Bạn ➜ " + toUser + "] " + message);
        }

        log("[PRIVATE][" + fromUser + " → " + toUser + "] " + message);
    }

    public void broadcastTypingIndicator(String username) {
        for (ClientHandler c : clients.values()) {
            String u = c.getUsername();
            if (u == null || u.equals(username)) continue;
            c.sendMessage("[TYPING]" + username);
        }
    }

    public void sendTypingIndicator(String fromUser, String toUser) {
        ClientHandler receiver = clients.get(toUser);
        if (receiver != null) {
            receiver.sendMessage("[TYPING]" + fromUser);
        }
    }

    // File transfer
    public void broadcastFile(String fromUser, String fileName, String base64Data, String excludeUser) {
        log("📎 " + fromUser + " → " + fileName);

        String fileMessage = "[FILE]" + fromUser + "|" + fileName + "|" + base64Data;

        for (ClientHandler c : clients.values()) {
            String u = c.getUsername();
            if (u == null) continue;
            if (excludeUser == null || !u.equals(excludeUser))
                c.sendMessage(fileMessage);
        }
    }

    public void sendPrivateFile(String fromUser, String toUser, String fileName, String base64Data) {
        ClientHandler receiver = clients.get(toUser);
        ClientHandler sender = clients.get(fromUser);

        String fileMessage = "[FILE]" + fromUser + "|" + fileName + "|" + base64Data;

        if (receiver != null) {
            receiver.sendMessage(fileMessage);
        }

        if (sender != null) {
            sender.sendMessage("[FILE]Bạn|" + fileName + "|sent");
        }

        log("📎 " + fromUser + " → " + toUser + ": " + fileName);
    }

    // Image transfer
    public void broadcastImage(String fromUser, String fileName, String base64Data, String excludeUser) {
        log("🖼️ " + fromUser + " → " + fileName);

        String imageMessage = "[IMAGE]" + fromUser + "|" + fileName + "|" + base64Data;

        for (ClientHandler c : clients.values()) {
            String u = c.getUsername();
            if (u == null) continue;
            if (excludeUser == null || !u.equals(excludeUser))
                c.sendMessage(imageMessage);
        }
    }

    public void sendPrivateImage(String fromUser, String toUser, String fileName, String base64Data) {
        ClientHandler receiver = clients.get(toUser);
        ClientHandler sender = clients.get(fromUser);

        String imageMessage = "[IMAGE]" + fromUser + "|" + fileName + "|" + base64Data;

        if (receiver != null) {
            receiver.sendMessage(imageMessage);
        }

        if (sender != null) {
            sender.sendMessage("[IMAGE]Bạn|" + fileName + "|sent");
        }

        log("🖼️ " + fromUser + " → " + toUser + ": " + fileName);
    }

    // Audio transfer
    public void broadcastAudio(String fromUser, String base64Audio, String excludeUser) {
        log("🎤 " + fromUser + " → Voice message");

        String audioMessage = "[AUDIO]" + fromUser + "|" + base64Audio;

        for (ClientHandler c : clients.values()) {
            String u = c.getUsername();
            if (u == null) continue;
            if (excludeUser == null || !u.equals(excludeUser))
                c.sendMessage(audioMessage);
        }
    }

    public void sendPrivateAudio(String fromUser, String toUser, String base64Audio) {
        ClientHandler receiver = clients.get(toUser);

        String audioMessage = "[AUDIO]" + fromUser + "|" + base64Audio;

        if (receiver != null) {
            receiver.sendMessage(audioMessage);
        }

        log("🎤 " + fromUser + " → " + toUser + ": Voice");
    }

    // Reactions
    public void broadcastReaction(String messageId, String reactor, String emoji) {
        // Silent - no log
        String reactionMessage = "[REACTION]" + messageId + "|" + reactor + "|" + emoji;

        for (ClientHandler c : clients.values()) {
            if (c.getUsername() != null) {
                c.sendMessage(reactionMessage);
            }
        }
    }

    public void sendPrivateReaction(String fromUser, String toUser, String messageId, String emoji) {
        ClientHandler receiver = clients.get(toUser);

        if (receiver != null) {
            receiver.sendMessage("[REACTION]" + messageId + "|" + fromUser + "|" + emoji);
        }
    }

    // Read receipts
    public void sendReadReceipt(String messageId, String reader, String originalSender) {
        // Silent - no log
        ClientHandler sender = clients.get(originalSender);

        if (sender != null) {
            sender.sendMessage("[READ]" + messageId + "|" + reader);
        }
    }

    // Reply
    public void broadcastReply(String fromUser, String replyToId, String message, String excludeUser) {
        String replyMessage = fromUser + " (reply): " + message;
        broadcastMessage(replyMessage, excludeUser);
        log("[REPLY] " + fromUser + " reply to " + replyToId + ": " + message);
    }

    public void sendPrivateReply(String fromUser, String toUser, String replyToId, String message) {
        String replyMessage = "↩️ " + message;
        sendPrivateMessage(fromUser, toUser, replyMessage);
        log("[REPLY PRIVATE][" + fromUser + " → " + toUser + "] reply: " + message);
    }

    // Message storage
    public void storeMessage(Message message) {
        messageStore.put(message.getId(), message);
    }

    public Message getMessage(String messageId) {
        return messageStore.get(messageId);
    }

    private void saveMessage(String msg) {
        chatHistory.add(msg);
        try (PrintWriter pw = new PrintWriter(new FileWriter("chat_history.txt", true))) {
            pw.println(msg);
        } catch (Exception ignored) {}
    }

    public List<String> getChatHistory() {
        return chatHistory;
    }

    public void receiveFromClient(String fromUser, String msg) {
        String logMsg = "[CLIENT→SERVER][" + fromUser + "] " + msg;
        saveMessage(logMsg);
        log(logMsg);
    }

    public void shutdownServer() {
        log("⚠️ Đang tắt server…");

        for (ClientHandler c : clients.values()) {
            c.sendMessage("[SERVER] Server đã tắt!");
            c.kick();
        }

        clients.clear();
        onlineInfo.clear();

        try {
            serverSocket.close();
        } catch (Exception ignored) {}

        log("✅ Server đã tắt.");
    }

    public static void main(String[] args) {
        new Server();
    }
}