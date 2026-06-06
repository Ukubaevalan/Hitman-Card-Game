package hitman;

import java.io.*;
import java.net.*;


public class TCPClient {
    public static void main(String[] args) throws IOException{
        Socket socket = new Socket("localhost", 8888);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        
        out.println("Hello from Client!"); 
        
        String message = in.readLine();     
        System.out.println("Server says: " + message);
        
        socket.close();
    }
}
