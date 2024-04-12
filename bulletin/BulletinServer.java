package bulletin;

import java.io.*;
import java.net.*;
import java.util.*;

public class BulletinServer {
    
    public static void main(String argv[]) throws Exception
    {
        // Set the port number
        int port = 6789;
        // Establish the listening socket
        ServerSocket serverSocket = new ServerSocket(port);
        // 
        Socket socket = null;
        //
        InputStreamReader iStreamReader = 

        // Process HTTP service requests in an infinite loop.
        while (true) {  
            // Listen for a TCP connection request.
            Socket connectionSocket = serverSocket.accept();

            // Construct an object to process the HTTP request message.
            HttpRequest request = new HttpRequest(connectionSocket);

            // Create a new thread to process the request.
            Thread thread = new Thread(request);

            // Start the thread.
            thread.start();
        }
    }
}
