package hitman;

import java.io.*;
import java.net.*;

public class TCPServer {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8888);
        System.out.println("Server started, waiting for clients...");

        while(true) {
        	Socket client = server.accept();
        	System.out.println("New Client is connected!");
        }
    }
}


class ClientHandler implements Runnable{
	private Socket client;
	
	public ClientHandler(Socket client) {
		this.client = client;
	}
	
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
			PrintWriter out = new PrintWriter(this.client.getOutputStream(), true);
			
			String message = in.readLine();
			System.out.println("Client says: " + message);
			out.println("Hello from server!");
			
			client.close();
		} catch(IOException e){
			System.out.println("Client disconnected, " + e.getMessage());
		}
	}
}

