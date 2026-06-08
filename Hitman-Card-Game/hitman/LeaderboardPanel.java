package hitman;

import java.awt.*;
import java.util.*;
import javax.swing.*;

public class LeaderboardPanel extends JPanel {
    private MyFrame frame;

    public LeaderboardPanel(MyFrame frame) {
        this.frame = frame;

        setBackground(new Color(20, 60, 20));
        setLayout(new BorderLayout(10, 10));

        // ---- TOP — title ----
        JLabel title = new JLabel("LEADERBOARD", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(255, 215, 0));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        // ---- CENTER — leaderboard entries ----
        JTextArea leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setBackground(new Color(20, 50, 20));
        leaderboardArea.setForeground(new Color(180, 255, 180));
        leaderboardArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        leaderboardArea.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // load and sort entries by wins
        FileManager fm = new FileManager();
        ArrayList<String> entries = fm.loadLeaderboard();

        // sort by wins descending
        entries.sort((a, b) -> {
            int winsA = Integer.parseInt(a.split(",")[1]);
            int winsB = Integer.parseInt(b.split(",")[1]);
            return winsB - winsA;
        });

        if(entries.isEmpty()) {
            leaderboardArea.setText("No games played yet!");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-20s %s%n", "PLAYER", "WINS"));
            sb.append("─".repeat(30) + "\n");
            int rank = 1;
            for(String entry : entries) {
                String[] parts = entry.split(",");
                sb.append(String.format("#%-3d %-20s %s%n", rank, parts[0], parts[1]));
                rank++;
            }
            leaderboardArea.setText(sb.toString());
        }

        JScrollPane scroll = new JScrollPane(leaderboardArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(100, 180, 100)));

        // ---- BOTTOM — back button ----
        JButton backBtn = new JButton("Back to Menu");
        backBtn.setBackground(new Color(180, 30, 30));
        backBtn.setForeground(Color.BLACK);
        backBtn.setFont(new Font("Arial", Font.BOLD, 14));
        backBtn.setOpaque(true);
        backBtn.setFocusPainted(false);
        backBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        backBtn.addActionListener(e -> frame.showPanel(new MainMenuPanel(frame)));

        add(title, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(backBtn, BorderLayout.SOUTH);
    }
}