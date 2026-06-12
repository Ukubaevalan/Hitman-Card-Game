package hitman;

import java.awt.*;
import javax.swing.*;

public class GameOverPanel extends JPanel {
    private MyFrame frame;

    public GameOverPanel(MyFrame frame, String winnerName) {
        this.frame = frame;

        // save winner to leaderboard
        FileManager fm = new FileManager();
        fm.saveWinner(winnerName);

        setBackground(new Color(20, 60, 20));
        setLayout(new BorderLayout(10, 10));

        // ---- TOP — game over title ----
        JLabel gameOverLabel = new JLabel("GAME OVER", SwingConstants.CENTER);
        gameOverLabel.setFont(new Font("Arial", Font.BOLD, 36));
        gameOverLabel.setForeground(new Color(255, 215, 0)); // gold
        gameOverLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 10, 0));

        // ---- CENTER — winner name ----
        JLabel winnerLabel = new JLabel(winnerName + " wins!", SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        winnerLabel.setForeground(Color.WHITE);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        centerPanel.setBackground(new Color(20, 60, 20));
        centerPanel.add(gameOverLabel);
        centerPanel.add(winnerLabel);

        // ---- BOTTOM — buttons ----
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        buttonPanel.setBackground(new Color(20, 60, 20));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        JButton playAgainBtn = new JButton("Play Again");
        playAgainBtn.setBackground(new Color(30, 100, 200));
        playAgainBtn.setForeground(Color.BLACK);
        playAgainBtn.setFont(new Font("Arial", Font.BOLD, 14));
        playAgainBtn.setOpaque(true);
        playAgainBtn.setFocusPainted(false);
        playAgainBtn.addActionListener(e -> 
            frame.showPanel(new PlayerSetupPanel(frame)));

        JButton leaderboardBtn = new JButton("Leaderboard");
        leaderboardBtn.setBackground(new Color(180, 140, 0));
        leaderboardBtn.setForeground(Color.BLACK);
        leaderboardBtn.setFont(new Font("Arial", Font.BOLD, 14));
        leaderboardBtn.setOpaque(true);
        leaderboardBtn.setFocusPainted(false);
        leaderboardBtn.addActionListener(e -> 
            frame.showPanel(new LeaderboardPanel(frame)));

        JButton menuBtn = new JButton("Main Menu");
        menuBtn.setBackground(new Color(180, 30, 30));
        menuBtn.setForeground(Color.BLACK);
        menuBtn.setFont(new Font("Arial", Font.BOLD, 14));
        menuBtn.setOpaque(true);
        menuBtn.setFocusPainted(false);
        menuBtn.addActionListener(e -> 
            frame.showPanel(new MainMenuPanel(frame)));

        buttonPanel.add(playAgainBtn);
        buttonPanel.add(leaderboardBtn);
        buttonPanel.add(menuBtn);

        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}