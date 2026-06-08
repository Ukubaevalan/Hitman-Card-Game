package hitman;

import java.util.*;

/**
 * NetworkGameState is the "translation layer" between the live Game object
 * and a plain text string that can travel over a TCP socket.
 *
 * FORMAT (one line, fields separated by '|'):
 *
 *   deckSize | currentPlayerIndex | p0Name:alive:extraTurns:card,card,... | p1Name:alive:extraTurns:card,card,... | lastLog | hitmanPosition
 *
 * Example:
 *   21|0|Alice:true:0:Angel,Future,Skip|Bob:true:0:Bomb,Stop,Angel|Alice drew a card.|NONE
 *
 * Why a flat string?
 *   Sockets send bytes, not objects. Java serialization would work too, but it
 *   is fragile across JVM versions. A plain text protocol is easy to debug
 *   (you can read it in the console), easy to parse, and requires no extra
 *   libraries — matching the project's existing style.
 *
 * The separator hierarchy:
 *   '|'  separates top-level fields
 *   ':'  separates fields within a player record
 *   ','  separates cards within a player's hand
 */
public class NetworkGameState {

    // -----------------------------------------------------------------------
    // SERIALIZATION  (Game → String)
    // -----------------------------------------------------------------------

    /**
     * Converts the live Game object into a single-line state string.
     *
     * @param game       the authoritative Game instance (lives on the server)
     * @param lastLog    the most recent log message to display on both screens
     * @param hitmanPos  "NONE" normally; "PENDING" when Angel placement is needed
     */
    public static String serialize(Game game, String lastLog, String hitmanPos) {
        StringBuilder sb = new StringBuilder();

        // Field 1: deck size
        sb.append(game.getDeckSize());
        sb.append("|");

        // Field 2: index of the player whose turn it currently is
        sb.append(game.currentPlayerIndex);
        sb.append("|");

        // Fields 3+: one record per player
        for (int i = 0; i < game.players.size(); i++) {
            Player p = game.players.get(i);
            sb.append(serializePlayer(p));
            sb.append("|");
        }

        // Second-to-last field: last log message
        // Replace any pipe characters in the message to avoid breaking the format
        sb.append(lastLog.replace("|", " "));
        sb.append("|");

        // Last field: hitman placement status
        sb.append(hitmanPos);

        return sb.toString();
    }

    /**
     * Converts a single Player into the format:  name:alive:extraTurns:card1,card2,...
     *
     * Example:  Alice:true:2:Angel,Skip,Future
     */
    private static String serializePlayer(Player p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getName()).append(":");
        sb.append(p.isAlive()).append(":");
        sb.append(p.hasExtraTurn() ? "1" : "0").append(":");

        // Hand — card names joined by commas; empty hand = empty string after the colon
        ArrayList<Card> hand = p.getHand();
        for (int i = 0; i < hand.size(); i++) {
            sb.append(hand.get(i).getName());
            if (i < hand.size() - 1) sb.append(",");
        }
        return sb.toString();
    }


    // -----------------------------------------------------------------------
    // DESERIALIZATION  (String → ParsedState POJO)
    // -----------------------------------------------------------------------

    /**
     * ParsedState is a simple data-holder (a "POJO" — Plain Old Java Object).
     * It stores exactly what GamePanel needs to redraw itself without having
     * access to the live Game object.
     *
     * Why not rebuild a full Game object?
     *   The client does not run game logic — it only displays state.
     *   Keeping a lightweight POJO avoids accidentally running game logic
     *   on the client, which would cause the two machines to diverge.
     */
    public static class ParsedState {
        public int deckSize;
        public int currentPlayerIndex;
        public String[] playerNames;
        public boolean[] playerAlive;
        public int[] playerExtraTurns;      // 0 or 1 (we only need to know if any exist)
        public ArrayList<String>[] playerHands;
        public String lastLog;
        public String hitmanPos;            // "NONE" or "PENDING"
        public int playerCount;
    }

    /**
     * Parses the state string produced by serialize() back into a ParsedState.
     *
     * @param stateStr the raw line received from the server
     * @return a ParsedState ready for GamePanel to consume
     */
    @SuppressWarnings("unchecked")
    public static ParsedState deserialize(String stateStr) {
        // Split on '|' — limit -1 ensures trailing empty strings are preserved
        String[] fields = stateStr.split("\\|", -1);

        ParsedState ps = new ParsedState();

        // Field 0: deck size
        ps.deckSize = Integer.parseInt(fields[0].trim());

        // Field 1: current player index
        ps.currentPlayerIndex = Integer.parseInt(fields[1].trim());

        // Fields 2 .. (n-3): player records
        // The last two fields are always lastLog and hitmanPos,
        // so player records occupy indices 2 through fields.length-3.
        ps.playerCount = fields.length - 4;  // total fields minus deckSize, currentIndex, lastLog, hitmanPos
        ps.playerNames     = new String[ps.playerCount];
        ps.playerAlive     = new boolean[ps.playerCount];
        ps.playerExtraTurns = new int[ps.playerCount];
        ps.playerHands     = new ArrayList[ps.playerCount];

        for (int i = 0; i < ps.playerCount; i++) {
            // Each player field: name:alive:extraTurns:card1,card2,...
            String[] parts = fields[2 + i].split(":", -1);

            ps.playerNames[i]      = parts[0];
            ps.playerAlive[i]      = Boolean.parseBoolean(parts[1]);
            ps.playerExtraTurns[i] = Integer.parseInt(parts[2]);

            ps.playerHands[i] = new ArrayList<>();
            if (parts.length > 3 && !parts[3].isEmpty()) {
                String[] cards = parts[3].split(",");
                for (String card : cards) {
                    ps.playerHands[i].add(card.trim());
                }
            }
        }

        // Second-to-last field: last log message
        ps.lastLog   = fields[fields.length - 2];

        // Last field: hitman placement status
        ps.hitmanPos = fields[fields.length - 1];

        return ps;
    }
}
