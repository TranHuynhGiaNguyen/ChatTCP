package chattcp;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private final BufferedReader in;
    private final PrintWriter out;
    public User user;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        try {
            login();
            showMenu();

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.trim().isEmpty()) continue;

                if (msg.startsWith("/")) {
                    handleCmd(msg);
                } else {
                    // Chat
                    if (user.getChatWith() != null) {
                        if (user.getChatWith().equalsIgnoreCase("SERVER")) {
                            server.receiveFromClient(user.getName(), msg);
                        } else {
                            server.sendPrivateMessage(user.getName(), user.getChatWith(), msg);
                        }
                    } else {
                        String formatted = user.getName() + ": " + msg;
                        server.broadcastMessage(formatted, null);
                    }
                }
            }
        } catch (IOException e) {
            disconnect();
        } finally {
            close();
        }
    }

    private void login() throws IOException {
        out.println("[SERVER] Nhập tên:");
        while (true) {
            String name = in.readLine().trim();
            if (name.isEmpty() || name.equalsIgnoreCase("SERVER")) {
                out.println("[SERVER] Tên không hợp lệ.");
                continue;
            }
            if (server.registerClient(name, this)) {
                this.user = new User(name);
                out.println("[SERVER] Chào " + name + "! Dùng /help để xem menu.");
                return;
            }
            out.println("[SERVER] Tên '" + name + "' đã tồn tại.");
        }
    }

    private void showMenu() {
        out.println("     CHAT TCP START        ");
        out.println();
        out.println("📝 CHAT CƠ BẢN:");
        out.println("  • Gõ tin nhắn để gửi");
        out.println("  • /list - xem danh sách online");
        out.println();
        out.println("💬 CHAT RIÊNG:");
        out.println("  • /to <tên> - gửi yêu cầu chat riêng");
        out.println("  • /accept - chấp nhận");
        out.println("  • /deny - từ chối");
        out.println("  • /back - về chat chung");
        out.println("  • /toserver - chat với server");
        out.println();
        out.println("📎 FILE & MEDIA:");
        out.println("  • /sendfile <tên>|<data> - gửi file");
        out.println("  • /sendimage <tên>|<data> - gửi ảnh");
        out.println("  • /sendaudio <data> - gửi voice");
        out.println();
        out.println("💡 TÍNH NĂNG:");
        out.println("  • /react <msgId>|<emoji> - react tin nhắn");
        out.println("  • /reply <msgId>|<text> - reply tin nhắn");
        out.println("  • /read <msgId> - đánh dấu đã đọc");
        out.println();
        out.println("🔧 KHÁC:");
        out.println("  • /history - xem lịch sử");
        out.println("  • /help - xem menu này");
        out.println("  • /exit - thoát");
        out.println();
        out.println();
    }

    private void handleCmd(String cmd) throws IOException {
        if (cmd.equals("/list")) {
            server.listClientNames(out);
        }
        else if (cmd.equals("/help")) {
            showMenu();
        }
        else if (cmd.startsWith("/to ")) {
            requestPM(cmd.split(" ", 2)[1]);
        }
        else if (cmd.equals("/accept")) {
            accept();
        }
        else if (cmd.equals("/deny")) {
            deny();
        }
        else if (cmd.equals("/back")) {
            back();
        }
        else if (cmd.equals("/history")) {
            sendChatHistory();
        }
        else if (cmd.equals("/toserver")) {
            user.setChatWith("SERVER");
            out.println("[SERVER] Bạn đang chat riêng với server. /back để thoát.");
        }
        else if (cmd.equals("/typing")) {
            handleTyping();
        }
        // FILE TRANSFER
        else if (cmd.startsWith("/sendfile ")) {
            handleSendFile(cmd.substring(10));
        }
        else if (cmd.startsWith("/sendimage ")) {
            handleSendImage(cmd.substring(11));
        }
        else if (cmd.startsWith("/sendaudio ")) {
            handleSendAudio(cmd.substring(11));
        }
        // REACTIONS
        else if (cmd.startsWith("/react ")) {
            handleReaction(cmd.substring(7));
        }
        // REPLY
        else if (cmd.startsWith("/reply ")) {
            handleReply(cmd.substring(7));
        }
        // READ RECEIPTS
        else if (cmd.startsWith("/read ")) {
            handleReadReceipt(cmd.substring(6));
        }
        else if (cmd.equals("/exit")) {
            exit();
        }
        else {
            out.println("[SERVER] ❌ Lệnh không hợp lệ. Gõ /help để xem hướng dẫn.");
        }
    }

    private void handleTyping() {
        if (user.getChatWith() != null && !user.getChatWith().equalsIgnoreCase("SERVER")) {
            server.sendTypingIndicator(user.getName(), user.getChatWith());
        } else if (user.getChatWith() == null) {
            server.broadcastTypingIndicator(user.getName());
        }
    }

    private void handleSendFile(String data) {
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) return;

        String fileName = parts[0];
        String base64Data = parts[1];

        if (user.getChatWith() != null && !user.getChatWith().equalsIgnoreCase("SERVER")) {
            server.sendPrivateFile(user.getName(), user.getChatWith(), fileName, base64Data);
        } else {
            server.broadcastFile(user.getName(), fileName, base64Data, null);
        }
    }

    private void handleSendImage(String data) {
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) return;

        String fileName = parts[0];
        String base64Data = parts[1];

        if (user.getChatWith() != null && !user.getChatWith().equalsIgnoreCase("SERVER")) {
            server.sendPrivateImage(user.getName(), user.getChatWith(), fileName, base64Data);
        } else {
            server.broadcastImage(user.getName(), fileName, base64Data, null);
        }
    }

    private void handleSendAudio(String base64Audio) {
        if (user.getChatWith() != null && !user.getChatWith().equalsIgnoreCase("SERVER")) {
            server.sendPrivateAudio(user.getName(), user.getChatWith(), base64Audio);
        } else {
            server.broadcastAudio(user.getName(), base64Audio, null);
        }
    }

    private void handleReaction(String data) {
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) return;

        String messageId = parts[0];
        String emoji = parts[1];

        if (user.getChatWith() != null && !user.getChatWith().equalsIgnoreCase("SERVER")) {
            server.sendPrivateReaction(user.getName(), user.getChatWith(), messageId, emoji);
        } else {
            server.broadcastReaction(messageId, user.getName(), emoji);
        }
    }

    private void handleReply(String data) {
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) return;

        String replyToId = parts[0];
        String message = parts[1];

        if (user.getChatWith() != null && !user.getChatWith().equalsIgnoreCase("SERVER")) {
            server.sendPrivateReply(user.getName(), user.getChatWith(), replyToId, message);
        } else {
            server.broadcastReply(user.getName(), replyToId, message, null);
        }
    }

    private void handleReadReceipt(String messageId) {
        // Silent - no server log
    }

    private void requestPM(String target) {
        if (user.getChatWith() != null) {
            out.println("[SERVER] Bạn đang chat riêng với " + user.getChatWith());
            return;
        }
        User t = server.getUser(target);
        if (t == null || t.getName().equals(user.getName())) {
            out.println("[SERVER] Không tìm thấy người nhận.");
            return;
        }
        t.setPending(user.getName());
        server.sendPrivateMessage(user.getName(), target,
                "[YÊU CẦU] " + user.getName() + " muốn chat riêng. /accept hoặc /deny");
        out.println("[SERVER] Đã gửi yêu cầu.");
    }

    private void accept() {
        String req = user.getPending();
        if (req == null) {
            out.println("[SERVER] Không có yêu cầu.");
            return;
        }
        User r = server.getUser(req);
        if (r == null) {
            out.println("[SERVER] User không tồn tại.");
            user.clearPending();
            return;
        }
        user.setChatWith(req);
        r.setChatWith(user.getName());
        user.clearPending();
        server.sendPrivateMessage("SERVER", req, user.getName() + " đã chấp nhận. Bắt đầu chat 2 chiều!");
        out.println("[SERVER] ✅ Chat 2 chiều với " + req + ". /back để thoát.");
    }

    private void deny() {
        String req = user.getPending();
        if (req != null) {
            server.sendPrivateMessage("SERVER", req, user.getName() + " đã từ chối.");
            user.clearPending();
            out.println("[SERVER] ❌ Đã từ chối yêu cầu.");
        }
    }

    private void back() {
        String chatWith = user.getChatWith();
        if (chatWith != null) {
            if (!chatWith.equalsIgnoreCase("SERVER")) {
                User u = server.getUser(chatWith);
                if (u != null) u.setChatWith(null);
                server.sendPrivateMessage("SERVER", chatWith, user.getName() + " đã thoát chat riêng.");
            }
            user.setChatWith(null);
            out.println("[SERVER] ✅ Đã về chat chung.");
        } else {
            out.println("[SERVER] ⚠️ Bạn đang ở chat chung.");
        }
    }

    private void sendChatHistory() {

        out.println("          LỊCH SỬ CHAT                  ");

        out.println();

        List<String> history = server.getChatHistory();

        if (history.isEmpty()) {
            out.println("  (Chưa có tin nhắn nào)");
        } else {
            int count = 0;
            int maxShow = 50; // Show last 50 messages
            int start = Math.max(0, history.size() - maxShow);

            for (int i = start; i < history.size(); i++) {
                out.println("  " + history.get(i));
                count++;
            }

            if (history.size() > maxShow) {
                out.println();
                out.println("  (Hiển thị " + count + "/" + history.size() + " tin nhắn gần đây)");
            }
        }

        out.println();

        out.println();
    }

    public void exit() {
        out.println("\n[SERVER] 👋 Tạm biệt! Hẹn gặp lại.");
        disconnect();
    }

    private void disconnect() {
        if (user != null) {
            server.removeClient(user.getName());
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String getUsername() {
        return user != null ? user.getName() : null;
    }

    public void kick() {
        sendMessage("[SERVER] 🚫 Bạn đã bị kick khỏi server!");
        close();
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}