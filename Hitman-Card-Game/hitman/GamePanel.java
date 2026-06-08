package hitman;

import java.util.*;
import java.awt.*;
import javax.swing.*;

public class GamePanel extends JPanel {
    private MyFrame frame;
    private Game game;

    private JLabel currentPlayerLabel;
    private JLabel deckSizeLabel;
    private JLabel lastCardLabel;
    private JPanel handPanel;
    private JTextArea gameLog;

    public GamePanel(MyFrame frame, Game game) {
        this.frame = frame;
        this.game = game;

        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(34, 85, 34)); // dark green table

        // ---- TOP — player info ----
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(new Color(20, 60, 20));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        currentPlayerLabel = new JLabel("", SwingConstants.CENTER);
        currentPlayerLabel.setForeground(Color.WHITE);
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 18));

        deckSizeLabel = new JLabel("", SwingConstants.CENTER);
        deckSizeLabel.setForeground(new Color(200, 200, 200));
        deckSizeLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        topPanel.add(currentPlayerLabel);
        topPanel.add(deckSizeLabel);
        add(topPanel, BorderLayout.NORTH);

        // ---- CENTER — last card + game log ----
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBackground(new Color(34, 85, 34));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        lastCardLabel = new JLabel("Last card played: None", SwingConstants.CENTER);
        lastCardLabel.setForeground(Color.WHITE);
        lastCardLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        centerPanel.add(lastCardLabel, BorderLayout.NORTH);

        gameLog = new JTextArea(8, 30);
        gameLog.setEditable(false);
        gameLog.setBackground(new Color(20, 50, 20));
        gameLog.setForeground(new Color(180, 255, 180));
        gameLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        gameLog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane logScroll = new JScrollPane(gameLog);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 180, 100)),
            "Game Log",
            0, 0,
            new Font("Arial", Font.BOLD, 12),
            Color.WHITE));
        centerPanel.add(logScroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // ---- BOTTOM — hand + draw button ----
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(new Color(20, 60, 20));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel handLabel = new JLabel("Your Hand:", SwingConstants.CENTER);
        handLabel.setForeground(Color.WHITE);
        handLabel.setFont(new Font("Arial", Font.BOLD, 13));
        bottomPanel.add(handLabel, BorderLayout.NORTH);

        handPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        handPanel.setBackground(new Color(20, 60, 20));
        JScrollPane handScroll = new JScrollPane(handPanel);
        handScroll.setPreferredSize(new Dimension(400, 100));
        handScroll.setBackground(new Color(20, 60, 20));
        handScroll.setBorder(null);
        bottomPanel.add(handScroll, BorderLayout.CENTER);

        JButton drawBtn = new JButton("Draw Card");
        drawBtn.setFont(new Font("Arial", Font.BOLD, 16));
        drawBtn.setBackground(new Color(180, 30, 30));
        drawBtn.setForeground(Color.WHITE);
        drawBtn.setFocusPainted(false);
        drawBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        drawBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        drawBtn.addActionListener(e -> drawCard());
        bottomPanel.add(drawBtn, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        refreshUI();
    }

    private Color getCardColor(String cardName) {
        switch(cardName) {
            case "Hitman":     return new Color(180, 30, 30);   // red
            case "Angel":      return new Color(255, 215, 0);   // gold
            case "Skip":       return new Color(30, 100, 200);  // blue
            case "Future":     return new Color(130, 40, 180);  // purple
            case "Bomb":       return new Color(200, 100, 0);   // orange
            case "Take Card":  return new Color(0, 150, 130);   // teal
            case "Stop":       return new Color(180, 0, 100);   // pink
            case "Shuffle":    return new Color(0, 150, 80);    // green
            case "Take Bottom":return new Color(100, 80, 40);   // brown
            default:           return new Color(80, 80, 80);    // gray
        }
    }

    private JButton createCardButton(Card c) {
        JButton btn = new JButton("<html><center>" + c.getName() + "</center></html>");
        btn.setPreferredSize(new Dimension(80, 70));
        btn.setOpaque(true);                          
        btn.setBorderPainted(true);                   
        btn.setBackground(getCardColor(c.getName()));
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> playCard(c));
        return btn;
    }

    private void drawCard() {
        try {
            Player current = game.getCurrentPlayer();
            Card returned = game.drawCard();

            if(returned != null && returned.getName().equals("Hitman")) {
            	String input = JOptionPane.showInputDialog(this,
            		    current.getName() + " was saved by Angel!\n" +
            		    "Deck has " + game.getDeckSize() + " cards.\n" +
            		    "Where to insert Hitman?\n" +
            		    "1 = top of deck, " + (game.getDeckSize() + 1) + " = bottom",
            		    "Place Hitman",
            		    JOptionPane.QUESTION_MESSAGE);

                if(input != null) {
                    try {
                        int position = Integer.parseInt(input.trim()) - 1;
                        game.getDeck().insertAt(position, returned);
                        log(current.getName() + " was saved by Angel!");
                    } catch(NumberFormatException ex) {
                        game.getDeck().insertAtRandom(returned);
                        log(current.getName() + " was saved by Angel!");
                    }
                } else {
                    game.getDeck().insertAtRandom(returned);
                    log(current.getName() + " was saved by Angel!");
                }
            } else {
                log(current.getName() + " drew a card.");
            }

            // check if current player was eliminated
            if(!current.isAlive()) {
                log(current.getName() + " was eliminated!");
            }

            Player winner = game.checkWinner();
            if(winner != null) {
                frame.showPanel(new GameOverPanel(frame, winner.getName()));
                return;
            }

            game.nextTurn();
            refreshUI();

        } catch(EmptyDeckException e) {
            log("Deck is empty — game over!");
        }
    }
    
    private void playCard(Card c) {
        Player current = game.getCurrentPlayer();

        switch(c.getName()) {
            case "Skip":
                try { current.playCard(c); } catch(InvalidPlayException ex) { log(ex.getMessage()); return; }
                log(current.getName() + " played Skip — turn skipped!");
                lastCardLabel.setText("Last card played: Skip");
                game.nextTurn();
                refreshUI();
                break;

            case "Shuffle":
                try { current.playCard(c); } catch(InvalidPlayException ex) { log(ex.getMessage()); return; }
                game.shuffleDeck();
                log(current.getName() + " played Shuffle — deck shuffled!");
                lastCardLabel.setText("Last card played: Shuffle");
                refreshUI();
                break;

            case "Future":
                try { current.playCard(c); } catch(InvalidPlayException ex) { 
                    log(ex.getMessage()); return; 
                }
                
                // check if any opponent wants to Stop the peek
                boolean stopped = false;
                for(Player opponent : game.getAlivePlayers()) {
                    if(tryStop(opponent, "Future")) {
                        stopped = true;
                        break;
                    }
                }
                
                if(!stopped) {
                    // no Stop played — show top 3
                    ArrayList<Card> top3 = game.peekTop3();
                    StringBuilder sb = new StringBuilder("Top 3 cards of deck:\n");
                    if(top3.isEmpty()) {
                        sb.append("Deck is empty!");
                    } else {
                        for(Card card : top3) {
                            sb.append("• " + card.getName() + "\n");
                        }
                    }
                    JOptionPane.showMessageDialog(this, sb.toString(), 
                        "Future — Top 3 Cards", JOptionPane.INFORMATION_MESSAGE);
                    log(current.getName() + " played Future.");
                    lastCardLabel.setText("Last card played: Future");
                }
                refreshUI();
                break;

            case "Take Bottom":
                try { current.playCard(c); } catch(InvalidPlayException ex) { log(ex.getMessage()); return; }
                try {
                    Card bottom = game.drawFromBottom();
                    current.addCard(bottom);
                    log(current.getName() + " played Take Bottom");
                    lastCardLabel.setText("Last card played: Take Bottom");
                } catch(EmptyDeckException ex) {
                    log("Deck is empty!");
                }
                refreshUI();
                break;

            case "Bomb":
                try { current.playCard(c); } catch(InvalidPlayException ex) { 
                    log(ex.getMessage()); return; 
                }
                Player bombTarget = selectTarget();
                if(bombTarget != null) {
                    // check if target wants to Stop
                    if(!tryStop(bombTarget, "Bomb")) {
                        // no Stop played — Bomb takes effect
                        game.bombPlayer(bombTarget, 2);
                        log(current.getName() + " played Bomb on " + bombTarget.getName() + "!");
                        lastCardLabel.setText("Last card played: Bomb");
                        game.forceTurn(bombTarget);
                        refreshUI();

                        boolean targetHasBomb = bombTarget.getHand()
                            .stream()
                            .anyMatch(card -> card.getName().equals("Bomb"));

                        if(targetHasBomb) {
                            int response = JOptionPane.showConfirmDialog(this,
                                bombTarget.getName() + " has a Bomb! Play it to counter?",
                                "Counter Bomb?",
                                JOptionPane.YES_NO_OPTION);

                            if(response == JOptionPane.YES_OPTION) {
                                Card counterBomb = bombTarget.getHand()
                                    .stream()
                                    .filter(card -> card.getName().equals("Bomb"))
                                    .findFirst()
                                    .orElse(null);

                                if(counterBomb != null) {
                                    try { bombTarget.playCard(counterBomb); }
                                    catch(InvalidPlayException ex) { log(ex.getMessage()); }
                                    game.bombPlayer(current, 2);
                                    bombTarget.clearExtraTurns();
                                    log(bombTarget.getName() + " countered with Bomb!");
                                    lastCardLabel.setText("Last card played: Bomb");
                                    game.forceTurn(current);
                                    refreshUI();
                                }
                            }
                        }
                    }
                }
                refreshUI();
                break;

            case "Take Card":
                try { current.playCard(c); } catch(InvalidPlayException ex) { 
                    log(ex.getMessage()); return; 
                }
                Player takeTarget = selectTarget();
                if(takeTarget != null) {
                    // check if target wants to Stop
                    if(!tryStop(takeTarget, "Take Card")) {
                        // no Stop — proceed with steal
                        if(takeTarget.getHand().isEmpty()) {
                            log(takeTarget.getName() + " has no cards to give!");
                        } else {
                            String[] cardNames = takeTarget.getHand()
                                .stream()
                                .map(Card::getName)
                                .toArray(String[]::new);

                            String chosen = (String) JOptionPane.showInputDialog(
                                this,
                                takeTarget.getName() + ", choose a card to give to " + current.getName() + ":",
                                "Give a Card",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                cardNames,
                                cardNames[0]);

                            if(chosen != null) {
                                Card given = takeTarget.getHand()
                                    .stream()
                                    .filter(card -> card.getName().equals(chosen))
                                    .findFirst()
                                    .orElse(null);

                                if(given != null) {
                                    takeTarget.removeCard(given);
                                    current.addCard(given);
                                    log(takeTarget.getName() + " gave a card to " + current.getName() + "!");
                                    lastCardLabel.setText("Last card played: Take Card");
                                }
                            }
                        }
                    }
                }
                refreshUI();
                break;

            case "Stop":
                log("Stop card — not yet implemented!");
                break;
        }
    }

    private Player selectTarget() {
        ArrayList<Player> targets = game.getAlivePlayers();
        if(targets.isEmpty()) {
            log("No targets available!");
            return null;
        }
        if(targets.size() == 1) return targets.get(0);

        String[] names = targets.stream().map(Player::getName).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(
            this,
            "Select target:",
            "Choose Target",
            JOptionPane.QUESTION_MESSAGE,
            null,
            names,
            names[0]);

        if(chosen == null) return null;
        return targets.stream().filter(p -> p.getName().equals(chosen)).findFirst().orElse(null);
    }
    
    private boolean tryStop(Player target, String actionName) {
        boolean hasStop = target.getHand()
            .stream()
            .anyMatch(card -> card.getName().equals("Stop"));

        if(!hasStop) return false;

        int response = JOptionPane.showConfirmDialog(this,
            target.getName() + " has a Stop card!\n" +
            "Play it to cancel " + actionName + "?",
            "Play Stop?",
            JOptionPane.YES_NO_OPTION);

        if(response == JOptionPane.YES_OPTION) {
            Card stopCard = target.getHand()
                .stream()
                .filter(card -> card.getName().equals("Stop"))
                .findFirst()
                .orElse(null);

            if(stopCard != null) {
                try { target.playCard(stopCard); }
                catch(InvalidPlayException ex) { log(ex.getMessage()); }
                log(target.getName() + " played Stop — action cancelled!");
                lastCardLabel.setText("Last card played: Stop");
                return true;
            }
        }
        return false;
    }

    private void refreshUI() {
        Player current = game.getCurrentPlayer();
        currentPlayerLabel.setText("Current Player: " + current.getName().toUpperCase());
        deckSizeLabel.setText("Deck: " + game.getDeckSize() + " cards remaining");

        current.sortHand();
        
        handPanel.removeAll();
        for(Card c : current.getHand()) {
            JButton cardBtn = createCardButton(c);
            handPanel.add(cardBtn);
        }
        handPanel.revalidate();
        handPanel.repaint();
    }

    private void log(String message) {
        gameLog.append("→ " + message + "\n");
        gameLog.setCaretPosition(gameLog.getDocument().getLength());
    }
}