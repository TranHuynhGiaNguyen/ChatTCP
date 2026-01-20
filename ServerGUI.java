package chattcp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ServerGUI extends JFrame {

    private Server server;

    private JTextArea logArea;
    private JList<String> onlineList;
    private DefaultListModel<String> onlineModel;

    private JButton btnStart, btnStop, btnKick, btnRefresh, btnPrivate, btnClear, btnBroadcast;
    private JTextField txtBroadcast;

    public ServerGUI() {
        setTitle("Chat TCP Server - Admin Panel");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // LEFT PANEL: ONLINE USERS
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(220, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Users Online"));

        onlineModel = new DefaultListModel<>();
        onlineList = new JList<>(onlineModel);
        leftPanel.add(new JScrollPane(onlineList), BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        // CENTER: LOG
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        add(scroll, BorderLayout.CENTER);

        // BOTTOM: CONTROLS
        JPanel bottom = new JPanel(new GridLayout(2, 1));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        btnStart = new JButton("Khởi động Server");
        btnStop = new JButton("Dừng Server");
        btnRefresh = new JButton("Làm mới");
        btnKick = new JButton("Kick User");
        btnPrivate = new JButton("Chat Private");
        btnClear = new JButton("Clear Log");

        row1.add(btnStart);
        row1.add(btnStop);
        row1.add(btnRefresh);
        row1.add(btnKick);
        row1.add(btnPrivate);
        row1.add(btnClear);

        bottom.add(row1);

        JPanel row2 = new JPanel(new BorderLayout());
        row2.add(new JLabel("Broadcast: "), BorderLayout.WEST);

        txtBroadcast = new JTextField();
        row2.add(txtBroadcast, BorderLayout.CENTER);

        btnBroadcast = new JButton("Broadcast");
        row2.add(btnBroadcast, BorderLayout.EAST);

        bottom.add(row2);

        add(bottom, BorderLayout.SOUTH);

        // Attach actions
        attachActions();
    }

    private void attachActions() {

        // START SERVER
        btnStart.addActionListener(e -> {
            if (server == null) {
                server = new Server(this);
                appendLog("[GUI] Server started.");
            } else {
                appendLog("[GUI] Server already running.");
            }
        });

        // STOP SERVER
        btnStop.addActionListener(e -> {
            if (server != null) {
                server.shutdownServer();
                server = null;
                onlineModel.clear();
                appendLog("[GUI] Server stopped.");
            }
        });

        // REFRESH ONLINE
        btnRefresh.addActionListener(e -> {
            if (server != null) {
                updateOnlineList(server.getOnlineNames());
            }
        });

        // KICK USER
        btnKick.addActionListener(e -> {
            if (server == null) return;

            String user = onlineList.getSelectedValue();
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Chọn 1 user để kick!");
                return;
            }

            server.kickUser(user);
            appendLog("[GUI] Kick user: " + user);
        });

        // PRIVATE MESSAGE
        btnPrivate.addActionListener(e -> {
            if (server == null) return;

            String user = onlineList.getSelectedValue();
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Chọn user để gửi tin riêng!");
                return;
            }

            String msg = JOptionPane.showInputDialog("Nhập tin nhắn gửi cho " + user + ":");
            if (msg != null && !msg.isEmpty()) {
                server.sendPrivateMessage("SERVER", user, msg);
                appendLog("[GUI -> " + user + "] " + msg);
            }
        });

        // BROADCAST
        btnBroadcast.addActionListener((ActionEvent e) -> {
            if (server == null) return;

            String msg = txtBroadcast.getText().trim();
            if (!msg.isEmpty()) {
                server.broadcastMessage("[SERVER] " + msg, null);
                appendLog("[SERVER BROADCAST] " + msg);
                txtBroadcast.setText("");
            }
        });

        // CLEAR LOG
        btnClear.addActionListener(e -> logArea.setText(""));
    }

    // Log to UI
    public void appendLog(String msg) {
        logArea.append(msg + "\n");
    }

    // Update online users list
    public void updateOnlineList(List<String> users) {
        onlineModel.clear();
        for (String u : users) onlineModel.addElement(u);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}