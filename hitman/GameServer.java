package hitman;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * GameServer runs on the HOST's machine.
 *
 * Responsibilities:
 *   1. Open a ServerSocket and wait for exactly 2 players to connect.
 *   2. Hold the single authoritative Game object — only the server ever
 *      mutates game state.
 *   3. Spawn one reader thread per client so both sockets are read
 *      simultaneously without either blocking the other.
 *   4. Parse incoming action messages, execute them on the Game, then
 *      broadcast the new serialized state to BOTH clients.
 *
 * -------------------------------------------------------------------------
 * MESSAGE PROTOCOL  (plain text, one line per message, '\n' terminated)
 * -------------------------------------------------------------------------
 *
 * Client → Server  (actions)
 *   DRAW                          player wants to draw the top card
 *   DRAW_BOTTOM                   player draws from the bottom of the deck
 *   PLAY:Skip                     play a Skip card
 *   PLAY:Shuffle                  play a Shuffle card
 *   PLAY:Future                   play a Future card (server responds with PEEK:...)
 *   PLAY:Bomb:TargetName          play Bomb targeting TargetName
 *   PLAY:TakeCard:TargetName:CardName   play Take Card, specifying what to steal
 *   PLAY:Stop                     play Stop (reactive, sent in response to QUERY_STOP)
 *   ANGEL_PLACE:position          after Angel save, place Hitman at given position
 *
 * Server → Client  (state + queries)
 *   STATE:...                     full serialized game state (always sent after any action)
 *   PEEK:card1,card2,card3        response to PLAY:Future — top 3 card names
 *   QUERY_STOP:ActionName         asks a client "do you want to play Stop?"
 *   STOP_RESPONSE:YES/NO          client replies to QUERY_STOP (reuses client→server direction)
 *   QUERY_ANGEL                   asks Angel-holder where to place Hitman
 *   WINNER:PlayerName             game over, this player won
 *   ELIMINATED:PlayerName         a player was eliminated
 *   YOUR_INDEX:0/1                sent once on connection — tells client which player slot they are
 * -------------------------------------------------------------------------
 */
public class GameServer {

    private static final int PORT = 8888;

    // The two client connection wrappers — index 0 = player 1, index 1 = player 2
    private ClientConnection[] clients = new ClientConnection[2];

    // The single authoritative game instance
    private Game game;

    // Protects game state from concurrent modification by the two reader threads
    private final Object gameLock = new Object();

    // Tracks whether a hitman card is pending placement (Angel was used)
    private volatile boolean hitmanPending = false;
    private volatile Card pendingHitman    = null;

    // -----------------------------------------------------------------------
    // Entry point called by NetworkMenuPanel after player enters their name
    // -----------------------------------------------------------------------
    public void start(String hostPlayerName, ServerReadyCallback onBothConnected) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("[Server] Listening on port " + PORT);

                // Accept player 1 (the host themselves — connecting via loopback or LAN)
                Socket s0 = serverSocket.accept();
                clients[0] = new ClientConnection(s0, 0);
                System.out.println("[Server] Player 1 connected.");

                // Accept player 2
                Socket s1 = serverSocket.accept();
                clients[1] = new ClientConnection(s1, 1);
                System.out.println("[Server] Player 2 connected.");

                // Read player names from each client (first message they send is their name)
                String name0 = clients[0].readLine();
                String name1 = clients[1].readLine();

                // Tell each client which index they are
                clients[0].send("YOUR_INDEX:0");
                clients[1].send("YOUR_INDEX:1");

                // Build the Game
                ArrayList<Player> players = new ArrayList<>();
                players.add(new Player(name0));
                players.add(new Player(name1));
                game = new Game(players);

                System.out.println("[Server] Game created: " + name0 + " vs " + name1);

                // Notify the UI that both players are connected and game has started
                if (onBothConnected != null) {
                    onBothConnected.onReady();
                }

                // Broadcast the initial state
                broadcastState("Game started! " + name0 + "'s turn.");

                // Start reader threads — one per client
                new Thread(() -> readLoop(0)).start();
                new Thread(() -> readLoop(1)).start();

            } catch (IOException e) {
                System.out.println("[Server] Error: " + e.getMessage());
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // Continuously reads messages from one client and dispatches them
    // -----------------------------------------------------------------------
    private void readLoop(int playerIndex) {
        try {
            String line;
            while ((line = clients[playerIndex].readLine()) != null) {
                System.out.println("[Server] From player " + playerIndex + ": " + line);
                synchronized (gameLock) {
                    handleMessage(line, playerIndex);
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] Player " + playerIndex + " disconnected: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Parses and executes a single action message
    // -----------------------------------------------------------------------
    private void handleMessage(String msg, int fromIndex) {
        String[] parts = msg.split(":", -1);
        String action  = parts[0].trim();
        Player actor   = game.players.get(fromIndex);

        // Safety: ignore actions from dead or wrong-turn players
        if (!actor.isAlive()) return;

        switch (action) {

            // ── DRAW ────────────────────────────────────────────────────────
            case "DRAW": {
                if (game.currentPlayerIndex != fromIndex) return; // not your turn
                try {
                    Card drawn = game.drawCard();
                    if (drawn != null && drawn.getName().equals("Hitman")) {
                        // Angel saved them — enter placement mode
                        hitmanPending = true;
                        pendingHitman = drawn;
                        broadcast("QUERY_ANGEL");
                        broadcastState(actor.getName() + " was saved by Angel! Place the Hitman.");
                    } else {
                        if (!actor.isAlive()) {
                            broadcastElimination(actor.getName());
                        } else {
                            checkWinnerAndAdvance(actor.getName() + " drew a card.");
                        }
                    }
                } catch (EmptyDeckException e) {
                    broadcast("STATE:" + NetworkGameState.serialize(game, "Deck is empty!", "NONE"));
                }
                break;
            }

            // ── ANGEL_PLACE ─────────────────────────────────────────────────
            case "ANGEL_PLACE": {
                if (!hitmanPending) return;
                int position = 0;
                try {
                    position = Integer.parseInt(parts[1].trim()) - 1;
                } catch (NumberFormatException e) {
                    // invalid input — insert randomly
                    position = -1;
                }
                if (position < 0) {
                    game.getDeck().insertAtRandom(pendingHitman);
                } else {
                    game.getDeck().insertAt(position, pendingHitman);
                }
                hitmanPending = false;
                pendingHitman = null;
                checkWinnerAndAdvance(actor.getName() + " placed the Hitman back in the deck.");
                break;
            }

            // ── PLAY ─────────────────────────────────────────────────────────
            case "PLAY": {
                if (game.currentPlayerIndex != fromIndex) return;
                String cardName = parts[1].trim();
                switch (cardName) {

                    case "Skip": {
                        try { actor.playCard(new SkipCard()); } catch (InvalidPlayException e) { return; }
                        game.skipTurn();
                        broadcastState(actor.getName() + " played Skip.");
                        break;
                    }

                    case "Shuffle": {
                        try { actor.playCard(new ShuffleCard()); } catch (InvalidPlayException e) { return; }
                        game.shuffleDeck();
                        broadcastState(actor.getName() + " shuffled the deck.");
                        break;
                    }

                    case "Future": {
                        try { actor.playCard(new FutureCard()); } catch (InvalidPlayException e) { return; }
                        ArrayList<Card> peeked = game.peekTop3();
                        StringBuilder peek = new StringBuilder("PEEK:");
                        for (int i = 0; i < peeked.size(); i++) {
                            peek.append(peeked.get(i).getName());
                            if (i < peeked.size() - 1) peek.append(",");
                        }
                        clients[fromIndex].send(peek.toString());
                        broadcastState(actor.getName() + " peeked at the top 3 cards.");
                        break;
                    }

                    case "TakeBottom": {
                        try { actor.playCard(new TakeBottomCard()); } catch (InvalidPlayException e) { return; }
                        try {
                            Card bottom = game.drawFromBottom();
                            actor.addCard(bottom);
                            broadcastState(actor.getName() + " drew from the bottom.");
                        } catch (EmptyDeckException e) {
                            broadcastState("Deck is empty!");
                        }
                        break;
                    }

                    case "Bomb": {
                        // PLAY:Bomb:TargetName
                        if (parts.length < 3) return;
                        String targetName = parts[2].trim();
                        Player target = getPlayerByName(targetName);
                        if (target == null || !target.isAlive()) return;

                        // Ask target if they want to Stop
                        int targetIndex = getPlayerIndex(target);
                        if (targetHasStop(target)) {
                            clients[targetIndex].send("QUERY_STOP:Bomb");
                            // We now wait for a STOP_RESPONSE — handled in the STOP_RESPONSE case
                            // Store pending bomb context
                            pendingBombActor  = actor;
                            pendingBombTarget = target;
                            pendingAction     = "Bomb";
                        } else {
                            applyBomb(actor, target);
                        }
                        break;
                    }

                    case "TakeCard": {
                        // PLAY:TakeCard:TargetName:CardName
                        if (parts.length < 4) return;
                        String targetName = parts[2].trim();
                        String cardToTake = parts[3].trim();
                        Player target = getPlayerByName(targetName);
                        if (target == null || !target.isAlive()) return;

                        int targetIndex = getPlayerIndex(target);
                        if (targetHasStop(target)) {
                            clients[targetIndex].send("QUERY_STOP:TakeCard");
                            pendingBombActor    = actor;
                            pendingBombTarget   = target;
                            pendingAction       = "TakeCard";
                            pendingStealCard    = cardToTake;
                        } else {
                            applyTakeCard(actor, target, cardToTake);
                        }
                        break;
                    }
                }
                break;
            }

            // ── STOP_RESPONSE ───────────────────────────────────────────────
            // The target replies YES or NO to a QUERY_STOP
            case "STOP_RESPONSE": {
                String answer = parts[1].trim();
                if (answer.equals("YES") && pendingBombTarget != null) {
                    // Target plays Stop — action is cancelled
                    try { pendingBombTarget.playCard(new StopCard()); } catch (InvalidPlayException e) { /* ignore */ }
                    broadcastState(pendingBombTarget.getName() + " played Stop — action cancelled!");
                    pendingBombActor  = null;
                    pendingBombTarget = null;
                    pendingAction     = null;
                    pendingStealCard  = null;
                } else {
                    // No Stop — apply the pending action
                    if ("Bomb".equals(pendingAction)) {
                        applyBomb(pendingBombActor, pendingBombTarget);
                    } else if ("TakeCard".equals(pendingAction)) {
                        applyTakeCard(pendingBombActor, pendingBombTarget, pendingStealCard);
                    }
                    pendingBombActor  = null;
                    pendingBombTarget = null;
                    pendingAction     = null;
                    pendingStealCard  = null;
                }
                break;
            }

            case "DRAW_BOTTOM": {
                if (game.currentPlayerIndex != fromIndex) return;
                try {
                    Card bottom = game.drawFromBottom();
                    if (bottom.getName().equals("Hitman")) {
                        if (actor.hasAngel()) {
                            actor.removeAngel();
                            hitmanPending = true;
                            pendingHitman = bottom;
                            broadcast("QUERY_ANGEL");
                            broadcastState(actor.getName() + " drew Hitman from bottom — Angel saved!");
                        } else {
                            actor.eliminate();
                            actor.getHand().clear();
                            broadcastElimination(actor.getName());
                        }
                    } else {
                        actor.addCard(bottom);
                        checkWinnerAndAdvance(actor.getName() + " drew from the bottom.");
                    }
                } catch (EmptyDeckException e) {
                    broadcastState("Deck is empty!");
                }
                break;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Pending action state (for Stop interactions)
    // -----------------------------------------------------------------------
    private Player pendingBombActor  = null;
    private Player pendingBombTarget = null;
    private String pendingAction     = null;
    private String pendingStealCard  = null;

    // -----------------------------------------------------------------------
    // Action helpers
    // -----------------------------------------------------------------------
    private void applyBomb(Player actor, Player target) {
        try { actor.playCard(new BombCard()); } catch (InvalidPlayException e) { return; }
        game.bombPlayer(target, 2);
        game.forceTurn(target);
        broadcastState(actor.getName() + " bombed " + target.getName() + "! They take 2 turns.");
    }

    private void applyTakeCard(Player actor, Player target, String cardName) {
        try { actor.playCard(new TakeCard()); } catch (InvalidPlayException e) { return; }
        // Find and transfer the card
        Card toGive = target.getHand().stream()
            .filter(c -> c.getName().equals(cardName))
            .findFirst().orElse(null);
        if (toGive != null) {
            target.removeCard(toGive);
            actor.addCard(toGive);
            broadcastState(target.getName() + " gave " + cardName + " to " + actor.getName() + ".");
        } else {
            broadcastState(target.getName() + " no longer has that card.");
        }
    }

    // -----------------------------------------------------------------------
    // Checks for a winner, advances the turn, and broadcasts state
    // -----------------------------------------------------------------------
    private void checkWinnerAndAdvance(String logMessage) {
        Player winner = game.checkWinner();
        if (winner != null) {
            broadcast("WINNER:" + winner.getName());
            broadcast("STATE:" + NetworkGameState.serialize(game, winner.getName() + " wins!", "NONE"));
        } else {
            game.nextTurn();
            broadcastState(logMessage);
        }
    }

    private void broadcastElimination(String name) {
        broadcast("ELIMINATED:" + name);
        Player winner = game.checkWinner();
        if (winner != null) {
            broadcast("WINNER:" + winner.getName());
        }
        broadcastState(name + " was eliminated!");
    }

    // -----------------------------------------------------------------------
    // Utility helpers
    // -----------------------------------------------------------------------
    private void broadcastState(String logMessage) {
        String state = "STATE:" + NetworkGameState.serialize(game, logMessage, hitmanPending ? "PENDING" : "NONE");
        broadcast(state);
    }

    private void broadcast(String message) {
        for (ClientConnection cc : clients) {
            if (cc != null) cc.send(message);
        }
    }

    private Player getPlayerByName(String name) {
        return game.players.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    private int getPlayerIndex(Player p) {
        for (int i = 0; i < game.players.size(); i++) {
            if (game.players.get(i) == p) return i;
        }
        return -1;
    }

    private boolean targetHasStop(Player target) {
        return target.getHand().stream().anyMatch(c -> c.getName().equals("Stop"));
    }

    // -----------------------------------------------------------------------
    // Callback interface — called when both clients are connected and the
    // game is built, so the host UI can transition to the game screen
    // -----------------------------------------------------------------------
    public interface ServerReadyCallback {
        void onReady();
    }

    // -----------------------------------------------------------------------
    // ClientConnection — wraps a Socket with a reader and writer
    // -----------------------------------------------------------------------
    private static class ClientConnection {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        final int index;

        ClientConnection(Socket socket, int index) throws IOException {
            this.socket = socket;
            this.index  = index;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        }

        String readLine() throws IOException {
            return reader.readLine();
        }

        void send(String message) {
            writer.println(message);
        }
    }
}
