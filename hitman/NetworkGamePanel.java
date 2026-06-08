package hitman;

import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * NetworkGamePanel is the network-aware replacement for GamePanel.
 *
 * Differences from GamePanel:
 *
 *   1. NO direct Game object — it never calls game.drawCard() etc.
 *      Instead it sends text messages to GameServer via GameClient.send().
 *      The server executes the action and sends back a STATE:... message,
 *      which this panel parses and renders.
 *
 *   2. TURN LOCKING — all action buttons are disabled when it is not this
 *      player's turn. isMyTurn() checks whether currentPlayerIndex in the
 *      received state matches myIndex.
 *
 *   3. OWN HAND ONLY — the hand panel shows this player's cards only.
 *      The opponent's hand is shown as a face-down card count.
 *
 *   4. IMPLEMENTS GameClient.MessageListener — the GameClient background
 *      thread calls onMessage() for every line received from the server.
 *      onMessage() switches on the message prefix and dispatches to the
 *      appropriate handler. All Swing mutations inside those handlers are
 *      wrapped in SwingUtilities.invokeLater() so they execute on the EDT.
 *
 * Angel placement:
 *   When the server broadcasts QUERY_ANGEL, the client whose turn it is
 *   shows a dialog asking where to insert the Hitman, then sends
 *   ANGEL_PLACE:position back to the server. The other client just sees
 *   the state update.
 */
public class NetworkGamePanel extends JPanel implements GameClient.MessageListener {

    private MyFrame    frame;
    private GameClient client;
    private int        myIndex;       // 0 or 1 — which player slot am I?

    // ── UI components ────────────────────────────────────────────────────
    private JLabel    currentPlayerLabel;
    private JLabel    deckSizeLabel;
    private JLabel    lastCardLabel;
    private JLabel    opponentInfoLabel;   // shows opponent name + card count
    private JPanel    handPanel;
    private JTextArea gameLog;
    private JButton   drawBtn;

    // ── Current parsed state ─────────────────────────────────────────────
    private NetworkGameState.ParsedState lastState = null;

    // ── Are we waiting for the Angel dialog to complete? ─────────────────
    private volatile boolean angelPending = false;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public NetworkGamePanel(MyFrame frame, GameClient client, int myIndex) {
        this.frame   = frame;
        this.client  = client;
        this.myIndex = myIndex;

        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(34, 85, 34));

        // ── TOP — current player + deck info ──────────────────────────────
        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.setBackground(new Color(20, 60, 20));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        currentPlayerLabel = new JLabel("Connecting…", SwingConstants.CENTER);
        currentPlayerLabel.setForeground(Color.WHITE);
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 18));

        deckSizeLabel = new JLabel("", SwingConstants.CENTER);
        deckSizeLabel.setForeground(new Color(200, 200, 200));
        deckSizeLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        opponentInfoLabel = new JLabel("", SwingConstants.CENTER);
        opponentInfoLabel.setForeground(new Color(255, 200, 100));
        opponentInfoLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        topPanel.add(currentPlayerLabel);
        topPanel.add(deckSizeLabel);
        topPanel.add(opponentInfoLabel);
        add(topPanel, BorderLayout.NORTH);

        // ── CENTER — last card + game log ──────────────────────────────────
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
            "Game Log", 0, 0,
            new Font("Arial", Font.BOLD, 12), Color.WHITE));
        centerPanel.add(logScroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // ── BOTTOM — your hand + draw button ───────────────────────────────
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

        drawBtn = new JButton("Draw Card");
        drawBtn.setFont(new Font("Arial", Font.BOLD, 16));
        drawBtn.setBackground(new Color(180, 30, 30));
        drawBtn.setForeground(Color.WHITE);
        drawBtn.setFocusPainted(false);
        drawBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        drawBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        drawBtn.setEnabled(false);   // disabled until we get first STATE
        drawBtn.addActionListener(e -> client.send("DRAW"));
        bottomPanel.add(drawBtn, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // -----------------------------------------------------------------------
    // MessageListener — called on the GameClient background thread
    // ALL Swing mutations go through invokeLater()
    // -----------------------------------------------------------------------
    @Override
    public void onMessage(String message) {
        if (message.startsWith("STATE:")) {
            String stateStr = message.substring(6); // strip "STATE:"
            NetworkGameState.ParsedState ps = NetworkGameState.deserialize(stateStr);
            SwingUtilities.invokeLater(() -> applyState(ps));

        } else if (message.startsWith("PEEK:")) {
            // Response to Future card — show dialog with top 3 card names
            String cards = message.substring(5);
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this,
                    "Top 3 cards of the deck:\n" + cards.replace(",", "\n"),
                    "Future — Peek",
                    JOptionPane.INFORMATION_MESSAGE));

        } else if (message.equals("QUERY_ANGEL")) {
            // Angel placement — only relevant to the player who drew Hitman
            if (lastState != null && lastState.currentPlayerIndex == myIndex) {
                SwingUtilities.invokeLater(this::showAngelDialog);
            }

        } else if (message.startsWith("QUERY_STOP:")) {
            String actionName = message.split(":")[1];
            SwingUtilities.invokeLater(() -> {
                int resp = JOptionPane.showConfirmDialog(this,
                    "Your opponent played " + actionName + "!\nPlay Stop to cancel?",
                    "Play Stop?",
                    JOptionPane.YES_NO_OPTION);
                client.send("STOP_RESPONSE:" + (resp == JOptionPane.YES_OPTION ? "YES" : "NO"));
            });

        } else if (message.startsWith("WINNER:")) {
            String winner = message.substring(7);
            SwingUtilities.invokeLater(() ->
                frame.showPanel(new GameOverPanel(frame, winner)));

        } else if (message.startsWith("ELIMINATED:")) {
            String eliminated = message.substring(11);
            SwingUtilities.invokeLater(() -> log(eliminated + " was eliminated!"));

        } else if (message.equals("DISCONNECTED")) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this,
                    "Connection lost. Returning to main menu.",
                    "Disconnected",
                    JOptionPane.ERROR_MESSAGE));
            SwingUtilities.invokeLater(() -> frame.showPanel(new MainMenuPanel(frame)));
        }
    }

    // -----------------------------------------------------------------------
    // Applies a parsed state to the UI
    // Must be called on the EDT
    // -----------------------------------------------------------------------
    private void applyState(NetworkGameState.ParsedState ps) {
        lastState = ps;

        boolean isMyTurn = (ps.currentPlayerIndex == myIndex);

        // ── Current player label ──────────────────────────────────────────
        String currentName = ps.playerNames[ps.currentPlayerIndex];
        String turnText = isMyTurn
            ? "YOUR TURN — " + ps.playerNames[myIndex].toUpperCase()
            : "Waiting for " + currentName.toUpperCase() + "…";
        currentPlayerLabel.setText(turnText);
        currentPlayerLabel.setForeground(isMyTurn ? new Color(100, 255, 100) : Color.WHITE);

        // ── Deck size ─────────────────────────────────────────────────────
        deckSizeLabel.setText("Deck: " + ps.deckSize + " cards remaining");

        // ── Opponent info (face-down card count) ──────────────────────────
        int opponentIndex = (myIndex == 0) ? 1 : 0;
        if (opponentIndex < ps.playerCount) {
            String opName   = ps.playerNames[opponentIndex];
            int    opCards  = ps.playerHands[opponentIndex].size();
            boolean opAlive = ps.playerAlive[opponentIndex];
            opponentInfoLabel.setText(
                opName + ": " + opCards + " card(s)" + (opAlive ? "" : " — ELIMINATED"));
        }

        // ── Game log ──────────────────────────────────────────────────────
        if (!ps.lastLog.isEmpty()) {
            log(ps.lastLog);
        }

        // ── Your hand ─────────────────────────────────────────────────────
        handPanel.removeAll();
        ArrayList<String> myHand = ps.playerHands[myIndex];
        // Sort hand alphabetically for consistency (mirrors Player.sortHand())
        Collections.sort(myHand);
        for (String cardName : myHand) {
            JButton cardBtn = createCardButton(cardName, isMyTurn);
            handPanel.add(cardBtn);
        }
        handPanel.revalidate();
        handPanel.repaint();

        // ── Draw button ───────────────────────────────────────────────────
        drawBtn.setEnabled(isMyTurn && !angelPending);
    }

    // -----------------------------------------------------------------------
    // Card button factory
    // -----------------------------------------------------------------------
    private JButton createCardButton(String cardName, boolean isMyTurn) {
        JButton btn = new JButton("<html><center>" + cardName + "</center></html>");
        btn.setPreferredSize(new Dimension(80, 70));
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setBackground(getCardColor(cardName));
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setEnabled(isMyTurn);
        btn.addActionListener(e -> handleCardPlay(cardName));
        return btn;
    }

    // -----------------------------------------------------------------------
    // Sends the correct PLAY:... message for each card type
    // -----------------------------------------------------------------------
    private void handleCardPlay(String cardName) {
        if (lastState == null) return;

        switch (cardName) {
            case "Skip":
                client.send("PLAY:Skip");
                break;

            case "Shuffle":
                client.send("PLAY:Shuffle");
                break;

            case "Future":
                client.send("PLAY:Future");
                break;

            case "Take Bottom":
                client.send("DRAW_BOTTOM");
                break;

            case "Bomb": {
                String target = selectOpponent("Select Bomb target:");
                if (target != null) client.send("PLAY:Bomb:" + target);
                break;
            }

            case "Take Card": {
                // First select target, then let target's dialog happen server-side
                String target = selectOpponent("Select Take Card target:");
                if (target == null) break;
                // We need to know what cards the target has — use lastState
                int targetIdx = getOpponentIndex();
                ArrayList<String> opHand = lastState.playerHands[targetIdx];
                if (opHand.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Opponent has no cards!", "Info", JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
                String[] cardArr = opHand.toArray(new String[0]);
                String chosen = (String) JOptionPane.showInputDialog(
                    this,
                    "Choose a card to steal from " + target + ":",
                    "Take Card",
                    JOptionPane.QUESTION_MESSAGE,
                    null, cardArr, cardArr[0]);
                if (chosen != null) client.send("PLAY:TakeCard:" + target + ":" + chosen);
                break;
            }

            case "Stop":
                log("Stop is played reactively — wait for an opponent's action.");
                break;

            case "Angel":
                log("Angel activates automatically when you draw Hitman.");
                break;

            default:
                log("Unknown card: " + cardName);
        }
    }

    // -----------------------------------------------------------------------
    // Angel placement dialog
    // -----------------------------------------------------------------------
    private void showAngelDialog() {
        angelPending = true;
        drawBtn.setEnabled(false);

        int deckSize = (lastState != null) ? lastState.deckSize : 1;
        String input = JOptionPane.showInputDialog(this,
            "You were saved by Angel!\nDeck has " + deckSize + " cards.\n" +
            "Where to place Hitman?\n1 = top, " + (deckSize + 1) + " = bottom",
            "Place Hitman",
            JOptionPane.QUESTION_MESSAGE);

        int position;
        try {
            position = (input != null) ? Integer.parseInt(input.trim()) : 0;
        } catch (NumberFormatException e) {
            position = 0; // 0 = random
        }

        client.send("ANGEL_PLACE:" + position);
        angelPending = false;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private String selectOpponent(String prompt) {
        if (lastState == null) return null;
        int opIdx = getOpponentIndex();
        if (!lastState.playerAlive[opIdx]) {
            log("No valid target.");
            return null;
        }
        return lastState.playerNames[opIdx]; // 2-player: only one possible target
    }

    private int getOpponentIndex() {
        return (myIndex == 0) ? 1 : 0;
    }

    private Color getCardColor(String cardName) {
        switch (cardName) {
            case "Hitman":      return new Color(180, 30, 30);
            case "Angel":       return new Color(255, 215, 0);
            case "Skip":        return new Color(30, 100, 200);
            case "Future":      return new Color(130, 40, 180);
            case "Bomb":        return new Color(200, 100, 0);
            case "Take Card":   return new Color(0, 150, 130);
            case "Stop":        return new Color(180, 0, 100);
            case "Shuffle":     return new Color(0, 150, 80);
            case "Take Bottom": return new Color(100, 80, 40);
            default:            return new Color(80, 80, 80);
        }
    }

    private void log(String message) {
        gameLog.append("→ " + message + "\n");
        gameLog.setCaretPosition(gameLog.getDocument().getLength());
    }
}
