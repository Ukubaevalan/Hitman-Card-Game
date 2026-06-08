package hitman;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

public class PlayerSetupPanel extends JPanel {
    private MyFrame frame;
    private JTextField player1Field;
    private JTextField player2Field;

    public PlayerSetupPanel(MyFrame frame) {
        this.frame = frame;

        // ---- Title ----
        JLabel title = new JLabel("Enter Player Names", SwingConstants.CENTER);

        // ---- Input fields ----
        JPanel inputPanel = new JPanel(new GridLayout(4, 1, 5, 10));

        JLabel p1Label = new JLabel("Player 1:", SwingConstants.CENTER);
        player1Field = new JTextField();

        JLabel p2Label = new JLabel("Player 2:", SwingConstants.CENTER);
        player2Field = new JTextField();

        inputPanel.add(p1Label);
        inputPanel.add(player1Field);
        inputPanel.add(p2Label);
        inputPanel.add(player2Field);

        // ---- Start button ----
        JButton startBtn = new JButton("Start Game");
        startBtn.addActionListener(e -> startGame());

        // ---- Layout ----
        setLayout(new BorderLayout(10, 10));
        add(title, BorderLayout.NORTH);
        add(inputPanel, BorderLayout.CENTER);
        add(startBtn, BorderLayout.SOUTH);
    }

    private void startGame() {
        String name1 = player1Field.getText().trim();
        String name2 = player2Field.getText().trim();

        // validate — names can't be empty
        if(name1.isEmpty() || name2.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both player names!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        } else if(name1.equals(name2)) {
        	JOptionPane.showMessageDialog(this, 
                    "Both players cannot have the same name!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
        	return;
        }

        // create players and game
        ArrayList<Player> players = new ArrayList<>();
        players.add(new Player(name1));
        players.add(new Player(name2));
        Game game = new Game(players);

        // switch to game screen
        frame.showPanel(new GamePanel(frame, game));
    }
}

