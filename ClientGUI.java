// ============================================================
// PART 1 of 3: ClientGUI - IMPORTS, VARIABLES, UI SETUP
// Copy toàn bộ nội dung này vào ClientGUI.java
// ============================================================

package chattcp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.UUID;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Base64;

public class ClientGUI extends JFrame {

    private JTextPane textPane;
    private JTextField inputField, searchField;
    private JButton sendBtn, emojiBtn, darkModeBtn, scrollBtn;
    private JButton fileBtn, imageBtn, voiceBtn, videoBtn, searchBtn;
    private StyledDocument doc;
    private JScrollPane scrollPane;
    private JLabel typingLabel, unreadBadge;
    private JPanel replyPanel;
    private JLabel replyLabel;

    // Buttons
    private JButton listBtn, privateBtn, acceptBtn, denyBtn, backBtn, historyBtn, helpBtn, exitBtn, toServerBtn;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    // Typing indicator
    private java.util.Timer typingTimer;
    private long lastTypingTime = 0;

    // Unread messages
    private int unreadCount = 0;
    private boolean isWindowFocused = true;

    // Dark mode
    private boolean isDarkMode = false;
    private Color bgColor = Color.WHITE;
    private Color fgColor = Color.BLACK;
    private Color panelColor = new Color(245, 245, 245);

    // Avatar colors
    private Map<String, Color> avatarColors = new HashMap<>();
    private Random colorRandom = new Random();

    // Messages storage
    private java.util.List<Message> messages = new java.util.ArrayList<>();
    private Message replyToMessage = null;

    // Voice recording
    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    private boolean isRecording = false;
    private ByteArrayOutputStream recordedAudio;

    // Search
    private boolean isSearching = false;
    private int currentSearchIndex = -1;
    private java.util.List<Integer> searchPositions = new java.util.ArrayList<>();

    public ClientGUI() {
        setTitle("Chat TCP Client ");
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initUI();
        setupListeners();
        setupAudio();
        connectToServer();
        setVisible(true);
    }

    /* ===================== UI SETUP ===================== */

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(new EmptyBorder(10, 10, 10, 10));
        main.setBackground(bgColor);

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(panelColor);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(panelColor);
        searchField = new JTextField(20);
        searchField.setVisible(false);
        searchBtn = new JButton("🔍 Search");
        JButton prevBtn = new JButton("↑");
        JButton nextBtn = new JButton("↓");
        JButton closeSearchBtn = new JButton("✕");
        prevBtn.setVisible(false);
        nextBtn.setVisible(false);
        closeSearchBtn.setVisible(false);

        searchPanel.add(searchBtn);
        searchPanel.add(searchField);
        searchPanel.add(prevBtn);
        searchPanel.add(nextBtn);
        searchPanel.add(closeSearchBtn);
        topPanel.add(searchPanel, BorderLayout.WEST);

        // Typing & Badge
        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        typingLabel.setForeground(Color.GRAY);

        unreadBadge = new JLabel("");
        unreadBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        unreadBadge.setForeground(Color.WHITE);
        unreadBadge.setOpaque(true);
        unreadBadge.setBackground(new Color(220, 53, 69));
        unreadBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        unreadBadge.setVisible(false);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(panelColor);
        rightPanel.add(typingLabel);
        rightPanel.add(unreadBadge);
        topPanel.add(rightPanel, BorderLayout.EAST);

        // Center - Chat area
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(bgColor);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textPane.setBackground(bgColor);
        textPane.setForeground(fgColor);
        doc = textPane.getStyledDocument();

        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    showMessageContextMenu(e.getX(), e.getY());
                }
            }
        });

        scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        scrollBtn = new JButton("↓ Tin mới");
        scrollBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        scrollBtn.setFocusPainted(false);
        scrollBtn.setBackground(new Color(0, 123, 255));
        scrollBtn.setForeground(Color.WHITE);
        scrollBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        scrollBtn.setVisible(false);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(scrollBtn, JLayeredPane.PALETTE_LAYER);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scrollPane.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                scrollBtn.setBounds(layeredPane.getWidth() - 120, layeredPane.getHeight() - 50, 100, 35);
            }
        });

        centerPanel.add(layeredPane, BorderLayout.CENTER);
        main.add(centerPanel, BorderLayout.CENTER);

        // Bottom - Input area
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBackground(panelColor);

        // Reply panel
        replyPanel = new JPanel(new BorderLayout());
        replyPanel.setBackground(new Color(230, 240, 255));
        replyPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        replyPanel.setVisible(false);
        replyLabel = new JLabel();
        replyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        JButton cancelReplyBtn = new JButton("✕");
        cancelReplyBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cancelReplyBtn.setBorderPainted(false);
        cancelReplyBtn.setContentAreaFilled(false);
        cancelReplyBtn.addActionListener(e -> {
            replyToMessage = null;
            replyPanel.setVisible(false);
        });
        replyPanel.add(replyLabel, BorderLayout.CENTER);
        replyPanel.add(cancelReplyBtn, BorderLayout.EAST);
        bottomContainer.add(replyPanel, BorderLayout.NORTH);

        // Input panel
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(panelColor);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel mediaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        mediaPanel.setBackground(panelColor);

        emojiBtn = new JButton("Icon");
        emojiBtn.setToolTipText("Emoji");
        fileBtn = new JButton("Gửi file");
        fileBtn.setToolTipText("Gửi file");
        imageBtn = new JButton("Gửi ảnh");
        imageBtn.setToolTipText("Gửi ảnh");
        voiceBtn = new JButton("Ghi âm");
        voiceBtn.setToolTipText("Ghi âm");
        videoBtn = new JButton("Call video");
        videoBtn.setToolTipText("Video call");

        for (JButton btn : new JButton[]{emojiBtn, fileBtn, imageBtn, voiceBtn, videoBtn}) {
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            btn.setFocusPainted(false);
            mediaPanel.add(btn);
        }

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sendBtn = new JButton("Gửi");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));

        bottomPanel.add(mediaPanel, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        bottomContainer.add(bottomPanel, BorderLayout.CENTER);
        main.add(bottomContainer, BorderLayout.SOUTH);

        // Command panel
        JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        commandPanel.setBackground(panelColor);

        listBtn = new JButton("Online");
        privateBtn = new JButton("Private");
        acceptBtn = new JButton("Accept");
        denyBtn = new JButton("Deny");
        backBtn = new JButton("Back");
        toServerBtn = new JButton("Chat với server");
        historyBtn = new JButton("Lịch sử chat");
        darkModeBtn = new JButton("🌙 Dark");
        helpBtn = new JButton("Trợ giúp");
        exitBtn = new JButton("Đăng xuất");

        for (JButton btn : new JButton[]{listBtn, privateBtn, acceptBtn, denyBtn, backBtn,
                toServerBtn, historyBtn, darkModeBtn, helpBtn, exitBtn}) {
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            commandPanel.add(btn);
        }

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(commandPanel, BorderLayout.NORTH);
        topContainer.add(topPanel, BorderLayout.SOUTH);
        main.add(topContainer, BorderLayout.NORTH);

        add(main);

        // Search listeners
        searchBtn.addActionListener(e -> {
            isSearching = !isSearching;
            searchField.setVisible(isSearching);
            prevBtn.setVisible(isSearching);
            nextBtn.setVisible(isSearching);
            closeSearchBtn.setVisible(isSearching);
            if (isSearching) searchField.requestFocus();
            else {
                searchPositions.clear();
                currentSearchIndex = -1;
            }
        });

        searchField.addActionListener(e -> performSearch(searchField.getText()));
        prevBtn.addActionListener(e -> navigateSearch(-1));
        nextBtn.addActionListener(e -> navigateSearch(1));
        closeSearchBtn.addActionListener(e -> {
            searchBtn.doClick();
            searchField.setText("");
        });
    }

    /* ===================== SETUP LISTENERS ===================== */

    private void setupListeners() {
        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendTypingIndicator();
            }
        });

        emojiBtn.addActionListener(e -> showEmojiPicker());
        fileBtn.addActionListener(e -> sendFile());
        imageBtn.addActionListener(e -> sendImage());
        voiceBtn.addActionListener(e -> toggleVoiceRecording());
        videoBtn.addActionListener(e -> startVideoCall());
        darkModeBtn.addActionListener(e -> toggleDarkMode());

        listBtn.addActionListener(e -> out.println("/list"));
        privateBtn.addActionListener(e -> {
            String target = JOptionPane.showInputDialog(this, "Nhập tên user muốn chat riêng:");
            if (target != null && !target.isEmpty()) out.println("/to " + target);
        });
        acceptBtn.addActionListener(e -> out.println("/accept"));
        denyBtn.addActionListener(e -> out.println("/deny"));
        backBtn.addActionListener(e -> out.println("/back"));
        toServerBtn.addActionListener(e -> {
            String msg = JOptionPane.showInputDialog(this, "Nhập tin gửi server:");
            if (msg != null) out.println("/toserver " + msg);
        });
        historyBtn.addActionListener(e -> out.println("/history"));
        helpBtn.addActionListener(e -> showHelp());
        exitBtn.addActionListener(e -> {
            out.println("/exit");
            System.exit(0);
        });

        scrollBtn.addActionListener(e -> {
            scrollToBottom();
            scrollBtn.setVisible(false);
            resetUnreadCount();
        });

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                isWindowFocused = true;
                resetUnreadCount();
            }
            @Override
            public void windowLostFocus(WindowEvent e) {
                isWindowFocused = false;
            }
        });

        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            int extent = scrollBar.getModel().getExtent();
            int maximum = scrollBar.getModel().getMaximum();
            int value = scrollBar.getValue();
            if (value + extent >= maximum - 10) {
                scrollBtn.setVisible(false);
                resetUnreadCount();
            }
        });
    }

    private void showHelp() {
        String help = "📖 HƯỚNG DẪN SỬ DỤNG v3.0\n\n" +
                "🎯 CHAT CƠ BẢN:\n• Gõ tin nhắn và Enter\n• Right-click để Reply/React\n• Click 😊 emoji\n\n" +
                "📁 FILE/ẢNH:\n• 📎 - Gửi file\n• 🖼️ - Gửi ảnh\n• 🎤 - Ghi âm\n• 📹 - Video call\n\n" +
                "🔍 SEARCH:\n• Click 🔍 Search\n• Nhập từ khóa\n• Dùng ↑↓\n\n" +
                "💬 CHAT RIÊNG:\n• 💬 Private\n• ✓ Accept\n• ✗ Deny\n• ← Back\n\n" +
                "⚙️ TÍNH NĂNG:\n• 🌙 Dark mode\n• ✓✓ Read receipts\n• 👍❤️😂 React\n• ↩️ Reply\n\n" +
                "📡 LỆNH:\n/list /to /accept /deny /back /history /help /exit";

        JTextArea textArea = new JTextArea(help);
        textArea.setEditable(false);
        JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, sp, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMessageContextMenu(int x, int y) {
        if (messages.isEmpty()) return;
        Message lastMsg = messages.get(messages.size() - 1);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem replyItem = new JMenuItem("↩️ Reply");
        replyItem.addActionListener(e -> {
            replyToMessage = lastMsg;
            replyLabel.setText("Trả lời: " + lastMsg.getContent());
            replyPanel.setVisible(true);
            inputField.requestFocus();
        });
        menu.add(replyItem);
        menu.addSeparator();

        String[] emojis = {"👍", "❤️", "😂", "😮", "😢", "🙏"};
        for (String emoji : emojis) {
            JMenuItem item = new JMenuItem(emoji);
            item.addActionListener(e -> sendReaction(lastMsg.getId(), emoji));
            menu.add(item);
        }
        menu.show(textPane, x, y);
    }

    private void showEmojiPicker() {
        String[] emojis = {
                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
                "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
                "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
                "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏",
                "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙",
                "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💪",
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘"
        };

        JPanel panel = new JPanel(new GridLayout(8, 8, 5, 5));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                inputField.setText(inputField.getText() + emoji);
                inputField.requestFocus();
                SwingUtilities.getWindowAncestor(panel).dispose();
            });
            panel.add(btn);
        }
        JDialog dialog = new JDialog(this, "Chọn Emoji", true);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void sendFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
                String b64 = Base64.getEncoder().encodeToString(data);
                out.println("/sendfile " + file.getName() + "|" + b64);
                append(" Đang gửi: " + file.getName(), true, username);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
            }
        }
    }

    private void sendImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
                String b64 = Base64.getEncoder().encodeToString(data);
                out.println("/sendimage " + file.getName() + "|" + b64);
                append(" Đang gửi: " + file.getName(), true, username);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
            }
        }
    }

    private void setupAudio() {
        audioFormat = new AudioFormat(16000.0F, 16, 1, true, false);
    }

    private void toggleVoiceRecording() {
        if (!isRecording) startRecording();
        else stopRecording();
    }

    private void startRecording() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            recordedAudio = new ByteArrayOutputStream();
            isRecording = true;
            voiceBtn.setText("⏹️");
            voiceBtn.setBackground(Color.RED);

            new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording) {
                    int count = targetDataLine.read(buffer, 0, buffer.length);
                    if (count > 0) recordedAudio.write(buffer, 0, count);
                }
            }).start();
            append("🎤 Đang ghi...", true, username);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
        }
    }

    private void stopRecording() {
        isRecording = false;
        voiceBtn.setText("🎤");
        voiceBtn.setBackground(null);
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        if (recordedAudio != null && recordedAudio.size() > 0) {
            byte[] data = recordedAudio.toByteArray();
            String b64 = Base64.getEncoder().encodeToString(data);
            out.println("/sendaudio " + b64);
            append(" Đã gửi (" + (data.length/1024) + "KB)", true, username);
        }
    }

    private void startVideoCall() {
        JOptionPane.showMessageDialog(this,
                " Video Call chưa hỗ trợ\nHiện có: Text, File, Image, Voice",
                "Video Call", JOptionPane.INFORMATION_MESSAGE);
    }

    private void performSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        searchPositions.clear();
        currentSearchIndex = -1;
        String text = textPane.getText().toLowerCase();
        String search = keyword.toLowerCase();
        int index = 0;
        while ((index = text.indexOf(search, index)) != -1) {
            searchPositions.add(index);
            index += search.length();
        }
        if (!searchPositions.isEmpty()) {
            currentSearchIndex = 0;
            highlightSearch(searchPositions.get(0), keyword.length());
            JOptionPane.showMessageDialog(this, "Tìm thấy " + searchPositions.size() + " kết quả");
        } else {
            JOptionPane.showMessageDialog(this, "Không tìm thấy '" + keyword + "'");
        }
    }

    private void navigateSearch(int dir) {
        if (searchPositions.isEmpty()) return;
        currentSearchIndex += dir;
        if (currentSearchIndex < 0) currentSearchIndex = searchPositions.size() - 1;
        if (currentSearchIndex >= searchPositions.size()) currentSearchIndex = 0;
        highlightSearch(searchPositions.get(currentSearchIndex), searchField.getText().length());
    }

    private void highlightSearch(int pos, int len) {
        try {
            textPane.requestFocus();
            textPane.setCaretPosition(pos);
            textPane.select(pos, pos + len);

            // Scroll to make selection visible
            Rectangle rect = textPane.modelToView(pos);
            if (rect != null) {
                rect.height = textPane.getVisibleRect().height;
                textPane.scrollRectToVisible(rect);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendReaction(String msgId, String emoji) {
        out.println("/react " + msgId + "|" + emoji);
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            bgColor = new Color(30, 30, 30);
            fgColor = new Color(220, 220, 220);
            panelColor = new Color(40, 40, 40);
            darkModeBtn.setText(" Light");
        } else {
            bgColor = Color.WHITE;
            fgColor = Color.BLACK;
            panelColor = new Color(245, 245, 245);
            darkModeBtn.setText(" Dark");
        }
        updateColors();
    }

    private void updateColors() {
        getContentPane().setBackground(bgColor);
        textPane.setBackground(bgColor);
        textPane.setForeground(fgColor);
        for (Component c : getContentPane().getComponents()) {
            updateComponentColors(c);
        }
        repaint();
    }

    private void updateComponentColors(Component c) {
        if (c instanceof JPanel) {
            c.setBackground(panelColor);
            for (Component ch : ((JPanel) c).getComponents()) {
                updateComponentColors(ch);
            }
        }
    }

    private void sendTypingIndicator() {
        long now = System.currentTimeMillis();
        if (now - lastTypingTime > 2000) {
            out.println("/typing");
            lastTypingTime = now;
        }
    }

    private void showTypingIndicator(String user) {
        typingLabel.setText(user + " đang gõ...");
        if (typingTimer != null) typingTimer.cancel();
        typingTimer = new java.util.Timer();
        typingTimer.schedule(new java.util.TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> typingLabel.setText(" "));
            }
        }, 3000);
    }

    private void incrementUnreadCount() {
        if (!isWindowFocused || !isScrolledToBottom()) {
            unreadCount++;
            updateUnreadBadge();
            scrollBtn.setVisible(true);
        }
    }

    private void resetUnreadCount() {
        unreadCount = 0;
        updateUnreadBadge();
    }

    private void updateUnreadBadge() {
        if (unreadCount > 0) {
            unreadBadge.setText(unreadCount + " tin mới");
            unreadBadge.setVisible(true);
            setTitle("(" + unreadCount + ") Chat");
        } else {
            unreadBadge.setVisible(false);
            setTitle("Chat ");
        }
    }

    private boolean isScrolledToBottom() {
        JScrollBar sb = scrollPane.getVerticalScrollBar();
        return sb.getValue() + sb.getModel().getExtent() >= sb.getModel().getMaximum() - 10;
    }

    /* ===================== CONNECT TO SERVER ===================== */

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1436);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            append("✅ Đã kết nối đến server.", false, null);

            new Thread(() -> {
                try {
                    String requestName = in.readLine();
                    if (requestName != null) append(requestName, false, null);

                    username = JOptionPane.showInputDialog(this, "Nhập tên của bạn:");
                    if (username == null || username.trim().isEmpty()) username = "User";

                    out.println(username);
                    startListening();

                } catch (Exception e) {
                    append("❌ Lỗi kết nối: " + e.getMessage(), false, null);
                }
            }).start();

        } catch (IOException e) {
            append("❌ Không kết nối được server.", false, null);
        }
    }

    /* ===================== START LISTENING ===================== */

    private void startListening() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("[TYPING]")) {
                        showTypingIndicator(line.substring(8).trim());
                        continue;
                    }
                    if (line.startsWith("[FILE]")) {
                        handleReceivedFile(line.substring(6));
                        continue;
                    }
                    if (line.startsWith("[IMAGE]")) {
                        handleReceivedImage(line.substring(7));
                        continue;
                    }
                    if (line.startsWith("[AUDIO]")) {
                        handleReceivedAudio(line.substring(7));
                        continue;
                    }
                    if (line.startsWith("[REACTION]")) {
                        handleReaction(line.substring(10));
                        continue;
                    }
                    if (line.startsWith("[READ]")) {
                        handleReadReceipt(line.substring(6));
                        continue;
                    }

                    boolean isOwnMessage = false;
                    String sender = null;

                    if (username != null && line.startsWith(username + ": ")) {
                        isOwnMessage = true;
                        sender = username;
                    } else if (line.startsWith("[Bạn ➜ ")) {
                        isOwnMessage = true;
                        sender = username;
                    } else {
                        if (line.contains(": ")) {
                            sender = line.substring(0, line.indexOf(": "));
                        } else if (line.startsWith("[") && line.contains("]")) {
                            int end = line.indexOf("]");
                            sender = line.substring(1, end);
                        }
                    }

                    String msgId = UUID.randomUUID().toString();
                    Message msg = new Message(msgId, sender != null ? sender : "Unknown", line, "text");
                    messages.add(msg);

                    append(line, isOwnMessage, sender);

                    if (!isOwnMessage) {
                        playNotificationSound();
                        incrementUnreadCount();
                        out.println("/read " + msgId);
                    }
                }
            } catch (IOException e) {
                append("❌ Mất kết nối server.", false, null);
            }
        }).start();
    }

    /* ===================== HANDLE RECEIVED MEDIA ===================== */

    private void handleReceivedFile(String data) {
        String[] parts = data.split("\\|", 3);
        if (parts.length < 3) return;

        String sender = parts[0];
        String fileName = parts[1];
        String base64Data = parts[2];

        append("📎 " + sender + " đã gửi file: " + fileName, false, sender);

        int choice = JOptionPane.showConfirmDialog(this,
                "Tải xuống file '" + fileName + "' từ " + sender + "?",
                "File nhận được", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                byte[] fileData = Base64.getDecoder().decode(base64Data);
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File(fileName));

                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    java.nio.file.Files.write(fc.getSelectedFile().toPath(), fileData);
                    JOptionPane.showMessageDialog(this, "✅ Đã lưu file!");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "❌ Lỗi: " + e.getMessage());
            }
        }
        playNotificationSound();
        incrementUnreadCount();
    }

    private void handleReceivedImage(String data) {
        String[] parts = data.split("\\|", 3);
        if (parts.length < 3) return;

        final String sender = parts[0];
        final String fileName = parts[1];
        final String base64Data = parts[2];

        try {
            byte[] imageData = Base64.getDecoder().decode(base64Data);
            ImageIcon icon = new ImageIcon(imageData);

            // Resize if too large
            if (icon.getIconWidth() > 300 || icon.getIconHeight() > 300) {
                Image img = icon.getImage();
                double scale = Math.min(300.0 / icon.getIconWidth(), 300.0 / icon.getIconHeight());
                int newW = (int)(icon.getIconWidth() * scale);
                int newH = (int)(icon.getIconHeight() * scale);
                Image scaledImg = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaledImg);
            }

            final ImageIcon finalIcon = icon;
            final byte[] finalImageData = imageData;

            SwingUtilities.invokeLater(() -> {
                try {
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);

                    String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                    String text = timestamp + " [" + getInitials(sender) + "] 🖼️ " + sender + ": " + fileName + "\n";

                    int len = doc.getLength();
                    doc.insertString(len, text, attrs);
                    doc.setParagraphAttributes(len, text.length(), attrs, false);

                    // Insert image
                    textPane.setCaretPosition(doc.getLength());

                    JLabel imageLabel = new JLabel(finalIcon);
                    imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                    imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    imageLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            // Show full size in dialog
                            ImageIcon fullIcon = new ImageIcon(finalImageData);
                            JLabel fullLabel = new JLabel(fullIcon);
                            JScrollPane scroll = new JScrollPane(fullLabel);
                            scroll.setPreferredSize(new Dimension(
                                    Math.min(fullIcon.getIconWidth() + 50, 800),
                                    Math.min(fullIcon.getIconHeight() + 50, 600)
                            ));
                            JOptionPane.showMessageDialog(ClientGUI.this, scroll, fileName, JOptionPane.PLAIN_MESSAGE);
                        }
                    });

                    textPane.insertComponent(imageLabel);
                    doc.insertString(doc.getLength(), "\n", attrs);

                    scrollToBottom();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            append(" " + sender + " đã gửi ảnh: " + fileName + " (Lỗi hiển thị)", false, sender);
        }

        playNotificationSound();
        incrementUnreadCount();
    }

    private void handleReceivedAudio(String data) {
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) return;

        String sender = parts[0];
        final String base64Audio = parts[1];

        JButton playBtn = new JButton("▶ Phát");
        playBtn.addActionListener(e -> {
            try {
                byte[] audioData = Base64.getDecoder().decode(base64Audio);
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream audioStream = new AudioInputStream(bais, audioFormat, audioData.length);

                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
                playBtn.setText("⏸ Đang phát...");

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        playBtn.setText("▶ Phát lại");
                    }
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "❌ Lỗi phát audio: " + ex.getMessage());
            }
        });

        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);

                String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                String text = timestamp + " [" + getInitials(sender) + "] 🎤 " + sender + " đã gửi tin nhắn thoại ";

                int len = doc.getLength();
                doc.insertString(len, text, attrs);
                doc.setParagraphAttributes(len, text.length(), attrs, false);

                // Insert play button component
                textPane.setCaretPosition(doc.getLength());
                textPane.insertComponent(playBtn);
                doc.insertString(doc.getLength(), "\n", attrs);

                scrollToBottom();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        playNotificationSound();
        incrementUnreadCount();
    }

    private void handleReaction(String data) {
        String[] parts = data.split("\\|", 3);
        if (parts.length < 3) return;

        String messageId = parts[0];
        String reactor = parts[1];
        String emoji = parts[2];

        for (Message msg : messages) {
            if (msg.getId().equals(messageId)) {
                msg.addReaction(reactor, emoji);
                break;
            }
        }
        append("   " + emoji + " " + reactor, false, null);
    }

    private void handleReadReceipt(String data) {
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) return;

        for (Message msg : messages) {
            if (msg.getId().equals(parts[0])) {
                msg.markReadBy(parts[1]);
                break;
            }
        }
    }

    /* ===================== SEND MESSAGE ===================== */

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        if (replyToMessage != null) {
            out.println("/reply " + replyToMessage.getId() + "|" + msg);
            replyToMessage = null;
            replyPanel.setVisible(false);
        } else {
            out.println(msg);
        }
        inputField.setText("");
    }

    /* ===================== DISPLAY MESSAGE ===================== */

    private void append(String msg, boolean isOwnMessage, String sender) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                String timestamp = new SimpleDateFormat("HH:mm").format(new Date());

                String displayMsg = msg;
                if (sender != null && !msg.startsWith("[SERVER]") && !msg.startsWith("📢") &&
                        !msg.startsWith("📎") && !msg.startsWith("🖼️") && !msg.startsWith("🎤")) {
                    displayMsg = timestamp + " [" + getInitials(sender) + "] " + msg;
                } else {
                    displayMsg = timestamp + " " + msg;
                }

                if (isOwnMessage) {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                    StyleConstants.setForeground(attrs, new Color(0, 102, 204));
                    StyleConstants.setBold(attrs, true);
                } else {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);

                    if (msg.startsWith("[SERVER]")) {
                        StyleConstants.setForeground(attrs, new Color(220, 53, 69));
                        StyleConstants.setBold(attrs, true);
                    } else if (msg.startsWith("[YÊU CẦU]") || msg.contains("➜")) {
                        StyleConstants.setForeground(attrs, new Color(255, 140, 0));
                    } else if (msg.startsWith("📢")) {
                        StyleConstants.setForeground(attrs, new Color(128, 128, 128));
                        StyleConstants.setItalic(attrs, true);
                    } else if (msg.startsWith("📎") || msg.startsWith("🖼️") || msg.startsWith("🎤")) {
                        StyleConstants.setForeground(attrs, new Color(0, 153, 0));
                    } else if (msg.startsWith("✅") || msg.startsWith("❌")) {
                        StyleConstants.setForeground(attrs, new Color(128, 128, 128));
                    } else {
                        StyleConstants.setForeground(attrs, isDarkMode ? fgColor : Color.BLACK);
                    }
                }

                int len = doc.getLength();
                doc.insertString(len, displayMsg + "\n", attrs);
                doc.setParagraphAttributes(len, displayMsg.length(), attrs, false);

                if (isScrolledToBottom() || isOwnMessage) {
                    scrollToBottom();
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /* ===================== UTILITIES ===================== */

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            textPane.setCaretPosition(doc.getLength());
        });
    }

    private Color getAvatarColor(String username) {
        if (!avatarColors.containsKey(username)) {
            float hue = colorRandom.nextFloat();
            avatarColors.put(username, Color.getHSBColor(hue, 0.6f, 0.8f));
        }
        return avatarColors.get(username);
    }

    private String getInitials(String username) {
        if (username == null || username.isEmpty()) return "?";
        if (username.length() == 1) return username.toUpperCase();
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private void playNotificationSound() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            // Ignore
        }
    }

    /* ===================== MAIN ===================== */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
