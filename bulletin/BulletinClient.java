package bulletin;

import java.io.*;
import java.net.*;
import java.util.*;

public class BulletinClient {
    
    public static void main(String[] args) {
        
        // Hardcoded port number
        int port = 6789;
        
        // Control socket
        Socket controlSocket = null;
        
        // InputStreamReader and OutputStreamWriter
        InputStreamReader isReader = null;
        OutputStreamWriter osWriter = null;
        
        // BufferedReader and BufferedWriter
        BufferedReader bReader = null;
        BufferedWriter bWriter = null;

        try {

            // Establish control socket
            controlSocket = new Socket("localhost", port);

            // get references to the socket input and output streams
            // isReader = new InputStreamReader(controlSocket.getInputStream());
            // osWriter = new OutputStreamWriter(controlSocket.getOutputStream());
            // bReader = new BufferedReader(isReader);
            // bWriter = new BufferedWriter(bWriter);
            bReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            bWriter = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));

            // reads input from keyboard
            Scanner scanner = new Scanner(System.in);




        }

    }

}
