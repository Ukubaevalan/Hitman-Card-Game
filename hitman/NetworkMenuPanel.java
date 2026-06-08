package hitman;

import java.awt.*;
import javax.swing.*;

/**
 * NetworkMenuPanel — Host vs Join selection screen.
 *
 * HOST flow:
 *   1. Player enters name → clicks "Host Game"
 *   2. Background thread starts GameServer (blocks waiting for 2 TCP connections)
 *   3. UI shows "Waiting for opponent…"
 *   4. Once both clients connect, server fires ServerReadyCallback
 *   5. Host connects their own GameClient to localhost, reads YOUR_INDEX, transitions to NetworkGamePanel
 *
 * JOIN flow:
 *   1. Player enters name + host IP → clicks "Join Game"
 *   2. Background thread connects GameClient to host IP
 *   3. Sends name, reads YOUR_INDEX via readOnce(), transitions to NetworkGamePanel
 *
 * Why readOnce()?
 *   startListening() spawns an infinite read loop. If we called it before reading
 *   YOUR_INDEX, that background thread would consume the YOUR_INDEX line before
 *   NetworkMenuPanel could see it. readOnce() reads exactly one line synchronously
 *   before the loop starts — no race condition.
 */
public class NetworkMenuPanel extends JPanel {

    private static final int PORT = 8888;
    private MyFrame frame;

    public NetworkMenuPanel(MyFrame frame) {
        this.frame = frame;

        setBackground(new Color(20, 60, 20));
        setLayout(new BorderLayout(10, 10));

        // ---- Title ----
        JLabel title = new JLabel("Network Game", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(255, 215, 0));
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));

        // ---- Input fields ----
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        inputPanel.setBackground(new Color(20, 60, 20));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        JLabel nameLabel = new JLabel("Your Name:", SwingConstants.RIGHT);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField nameField = new JTextField();

        JLabel ipLabel = new JLabel("Host IP (join only):", SwingConstants.RIGHT);
        ipLabel.setForeground(Color.WHITE);
        ipLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField ipField = new JTextField("localhost");

        inputPanel.add(nameLabel); inputPanel.add(nameField);
        inputPanel.add(ipLabel);   inputPanel.add(ipField);

        // ---- Status label ----
        JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(180, 255, 180));
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 13));

        // ---- Buttons ----
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        buttonPanel.setBackground(new Color(20, 60, 20));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));

        JButton hostBtn = makeButton("Host Game", new Color(30, 100, 200));
        JButton joinBtn = makeButton("Join Game",  new Color(0, 150, 80));
        JButton backBtn = makeButton("Back",        new Color(180, 30, 30));

        buttonPanel.add(hostBtn);
        buttonPanel.add(joinBtn);
        buttonPanel.add(backBtn);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        centerPanel.setBackground(new Color(20, 60, 20));
        centerPanel.add(inputPanel);
        centerPanel.add(statusLabel);
        centerPanel.add(buttonPanel);

        add(title,       BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // ── HOST ──────────────────────────────────────────────────────────
        hostBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { error("Please enter your name."); return; }

            hostBtn.setEnabled(false);
            joinBtn.setEnabled(false);
            statusLabel.setText("Waiting for opponent on port " + PORT + "…");

            GameServer server = new GameServer();

            new Thread(() -> {
                // Start server — blocks until 2 clients connect
                server.start(name, () -> {
                    // Both connected. Now host connects their client to localhost.
                    try {
                        GameClient client = new GameClient();
                        client.connect("localhost", PORT);
                        client.send(name);

                        // Read YOUR_INDEX (server sends this immediately after name)
                        String indexLine = client.readOnce();
                        int myIndex = parseIndex(indexLine);
                        client.setMyIndex(myIndex);

                        // Transition to game panel on EDT
                        SwingUtilities.invokeLater(() -> {
                            NetworkGamePanel ngp = new NetworkGamePanel(frame, client, myIndex);
                            client.setListener(ngp);
                            client.startListening();
                            frame.showPanel(ngp);
                        });

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() ->
                            statusLabel.setText("Error: " + ex.getMessage()));
                    }
                });
            }).start();
        });

        // ── JOIN ──────────────────────────────────────────────────────────
        joinBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip   = ipField.getText().trim();

            if (name.isEmpty()) { error("Please enter your name."); return; }
            if (ip.isEmpty())   { error("Please enter the host's IP address."); return; }

            hostBtn.setEnabled(false);
            joinBtn.setEnabled(false);
            statusLabel.setText("Connecting to " + ip + ":" + PORT + "…");

            new Thread(() -> {
                try {
                    GameClient client = new GameClient();
                    client.connect(ip, PORT);
                    client.send(name);

                    String indexLine = client.readOnce();
                    int myIndex = parseIndex(indexLine);
                    client.setMyIndex(myIndex);

                    SwingUtilities.invokeLater(() -> {
                        NetworkGamePanel ngp = new NetworkGamePanel(frame, client, myIndex);
                        client.setListener(ngp);
                        client.startListening();
                        frame.showPanel(ngp);
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Could not connect: " + ex.getMessage());
                        hostBtn.setEnabled(true);
                        joinBtn.setEnabled(true);
                    });
                }
            }).start();
        });

        backBtn.addActionListener(e -> frame.showPanel(new MainMenuPanel(frame)));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private JButton makeButton(String label, Color bg) {
        JButton btn = new JButton(label);
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        return btn;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private int parseIndex(String msg) {
        if (msg != null && msg.startsWith("YOUR_INDEX:")) {
            try { return Integer.parseInt(msg.split(":")[1].trim()); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}

