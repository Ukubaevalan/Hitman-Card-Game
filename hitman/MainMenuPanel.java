package hitman;

import java.awt.*;
import javax.swing.*;

/**
 * MainMenuPanel — updated to include a Network Game option.
 *
 * The only change from the original is adding a fourth button "Network Game"
 * that routes to NetworkMenuPanel, and adjusting the GridLayout from 3x1 to 4x1.
 * All existing buttons (Start Game, Leaderboard, Exit) are unchanged.
 */
public class MainMenuPanel extends JPanel {
    private MyFrame frame;

    public MainMenuPanel(MyFrame frame) {
        this.frame = frame;

        // ---- Title ----
        JLabel title = new JLabel("Hitman Card Game", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(255, 215, 0));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        setBackground(new Color(20, 60, 20));

        // ---- Buttons ---- (4x1 grid — added Network Game)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1, 0, 10));
        buttonPanel.setBackground(new Color(20, 60, 20));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 40, 60));

        JButton startBtn   = makeButton("Start Game (Local)",  new Color(30, 100, 200));
        JButton networkBtn = makeButton("Network Game",         new Color(0, 150, 80));
        JButton leaderBtn  = makeButton("Leaderboard",          new Color(180, 140, 0));
        JButton exitBtn    = makeButton("Exit",                 new Color(180, 30, 30));

        buttonPanel.add(startBtn);
        buttonPanel.add(networkBtn);
        buttonPanel.add(leaderBtn);
        buttonPanel.add(exitBtn);

        // ---- Listeners ----
        startBtn.addActionListener(e   -> frame.showPanel(new PlayerSetupPanel(frame)));
        networkBtn.addActionListener(e -> frame.showPanel(new NetworkMenuPanel(frame)));
        leaderBtn.addActionListener(e  -> frame.showPanel(new LeaderboardPanel(frame)));
        exitBtn.addActionListener(e    -> System.exit(0));

        // ---- Layout ----
        setLayout(new BorderLayout());
        add(title,       BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }

    private JButton makeButton(String label, Color bg) {
        JButton btn = new JButton(label);
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        return btn;
    }
}

