package hitman;

import java.io.*;
import java.net.*;

/**
 * GameClient runs on BOTH machines (host connects via loopback; joining player
 * connects via the host's IP).
 *
 * Key design decisions:
 *
 *  1. BACKGROUND READER THREAD
 *     readLine() blocks until data arrives. If we called it on the Swing EDT
 *     the whole UI would freeze. We run it on a daemon thread and deliver
 *     each received line to a MessageListener callback. The listener (NetworkGamePanel)
 *     then uses SwingUtilities.invokeLater() to safely touch Swing components.
 *
 *  2. BLOCKING readOnce() FOR HANDSHAKE
 *     During connection setup (before startListening() is called), the server
 *     sends exactly one line: "YOUR_INDEX:0" or "YOUR_INDEX:1". We expose
 *     readOnce() so NetworkMenuPanel can read that single line synchronously
 *     on a background thread — no race condition because startListening() has
 *     not been called yet.
 *
 *  3. INTENTIONALLY DUMB — no game logic lives here. All decisions happen in GameServer.
 */
public class GameClient {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter   writer;

    private MessageListener listener;
    private int myIndex = -1;

    // -----------------------------------------------------------------------
    // Listener interface
    // -----------------------------------------------------------------------
    public interface MessageListener {
        void onMessage(String message);
    }

    // -----------------------------------------------------------------------
    // Connect — call on a background thread, it blocks until server accepts
    // -----------------------------------------------------------------------
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        System.out.println("[Client] Connected to " + host + ":" + port);
    }

    // -----------------------------------------------------------------------
    // Handshake read — call ONCE before startListening()
    // Returns the next line from the server (blocks until it arrives).
    // -----------------------------------------------------------------------
    public String readOnce() throws IOException {
        return reader.readLine();
    }

    // -----------------------------------------------------------------------
    // Register the UI callback
    // -----------------------------------------------------------------------
    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    // -----------------------------------------------------------------------
    // Start the background reader loop
    // -----------------------------------------------------------------------
    public void startListening() {
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String msg = line;
                    System.out.println("[Client] Received: " + msg);
                    if (listener != null) listener.onMessage(msg);
                }
            } catch (IOException e) {
                System.out.println("[Client] Connection closed: " + e.getMessage());
                if (listener != null) listener.onMessage("DISCONNECTED");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // -----------------------------------------------------------------------
    // Send an action to the server
    // -----------------------------------------------------------------------
    public void send(String message) {
        if (writer != null) {
            writer.println(message);
            System.out.println("[Client] Sent: " + message);
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public int getMyIndex()        { return myIndex; }
    public void setMyIndex(int i)  { this.myIndex = i; }

    // -----------------------------------------------------------------------
    // Disconnect
    // -----------------------------------------------------------------------
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("[Client] Error closing: " + e.getMessage());
        }
    }
}
