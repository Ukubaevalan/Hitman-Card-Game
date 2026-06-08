package hitman;

import java.awt.*;
import javax.swing.*;

public class MainMenuPanel extends JPanel {
    private MyFrame frame;

    public MainMenuPanel(MyFrame frame) {
        this.frame = frame;

        // ---- Title ----
        JLabel title = new JLabel("Hitman Card Game", SwingConstants.CENTER);

        // ---- Buttons ----
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1));

        JButton startBtn = new JButton("Start Game");
        JButton leaderBtn = new JButton("Leaderboard");
        JButton exitBtn = new JButton("Exit");

        buttonPanel.add(startBtn);
        buttonPanel.add(leaderBtn);
        buttonPanel.add(exitBtn);

        // ---- Listeners ----
        startBtn.addActionListener(e -> frame.showPanel(new PlayerSetupPanel(frame)));
        leaderBtn.addActionListener(e -> frame.showPanel(new LeaderboardPanel(frame)));
        exitBtn.addActionListener(e -> System.exit(0));

        // ---- Layout ----
        setLayout(new BorderLayout());
        add(title, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }
}