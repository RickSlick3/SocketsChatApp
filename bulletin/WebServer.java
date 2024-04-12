/**
* Programming Project 1
* RICHARD ROBERTS
*/

package bulletin;

import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer {   
    public static void main(String argv[]) throws Exception
    {
        // Set the port number
        int port = 6789;

        // Establish the listening socket
        ServerSocket welcomeSocket = new ServerSocket(port);

        // Process HTTP service requests in an infinite loop.
        while (true) {  
            // Listen for a TCP connection request.
            Socket connectionSocket = welcomeSocket.accept();

            // Construct an object to process the HTTP request message.
            HttpRequest request = new HttpRequest(connectionSocket);

            // Create a new thread to process the request.
            Thread thread = new Thread(request);

            // Start the thread.
            thread.start();
        }
    }
}

final class HttpRequest implements Runnable
{
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor
    public HttpRequest(Socket socket) throws Exception
    {
        this.socket = socket;
    }

    // Implement the run() method of the Runnable interface.
    public void run()
    {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Return the MIME-type of the file
    /**private String contentType(String fileName)
    {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        return fileNameMap.getContentTypeFor(fileName);
    } */

    private static String contentType(String fileName)
    {
        if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        if(fileName.endsWith(".gif")) {
            return "image/gif";
        }
        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".jpe")) {
            return "image/jpeg";
        } 
        if (fileName.endsWith(".png")) {
            return "image/png";
        } 
        if(fileName.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;
        
        // Copy requested file into the socket's output stream.
        while((bytes = fis.read(buffer)) != -1 ) {
            os.write(buffer, 0, bytes);
        }
    }


    private void processRequest() throws Exception
    {
        // Get a reference to the socket's input and output streams.
        InputStream is = this.socket.getInputStream();
        OutputStream outs = this.socket.getOutputStream();

        // Set up input stream filters.
        DataOutputStream os = new DataOutputStream(outs);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        // Get the request line of the HTTP request message.
        String requestLine = br.readLine();
        
        // Display the request line.
        System.out.println("\nREQUEST:\n------------------------------");
        System.out.println(requestLine);

        // Get and display the header lines.
        String headerLine = null;
        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // USE THESE 3 LINES TO TEST ALL CODE ABOVE THIS LINE

        // Close streams and socket.   
        /**    
        os.close();
        br.close();
        socket.close();
        */
        
        // Extract the filename from the request line.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken(); // skip over the method, which should be "GET"
        String fileName = tokens.nextToken();

        // Prepend a "." so that file request is within the current directory.
        fileName = "." + fileName;

        // Open the requested file.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }

        // Construct the response message.
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        
        // Code for Task 1
        // if (fileExists) {
        //     statusLine = "HTTP/1.1 200 OK";
        //     contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
        // } else {
        //     statusLine = "HTTP/1.1 404 Not Found";
        //     contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
        //     entityBody = "<HTML>" + 
        //                 "<HEAD><TITLE>Not Found</TITLE></HEAD>" + 
        //                 "<BODY>Not Found</BODY></HTML>";
        // }

        // Code for Task 2
        if (fileExists) {
            statusLine = "HTTP/1.1 200 OK";
            contentTypeLine = "Content-type: " + contentType( fileName ) + CRLF;
        } else {
            // if the file requested is any type other than a text (.txt) file, report
            // error to the web client
            if (!contentType(fileName).equalsIgnoreCase("text/plain")) {
                statusLine = "HTTP/1.1 404 Not Found";
                contentTypeLine = "Content-type: " + contentType( fileName ) + CRLF;
                entityBody = "<HTML>" +
                "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
                "<BODY>Not Found</BODY></HTML>";
            } else { // else retrieve the text (.txt) file from your local FTP server
                statusLine = "HTTP/1.1 200 OK";
                contentTypeLine = "Content-type: text/plain" + CRLF;
                // create an instance of ftp client
                FtpClient client = new FtpClient();
                // connect to the ftp server
                client.connect("ricky", "abc");
                // retrieve the file from the ftp server, remember you need to
                // first upload this file to the ftp server under your user
                // ftp directory
                client.getFile(fileName);
                // disconnect from ftp server
                client.disconnect();
                // assign input stream to read the recently ftp-downloaded file
                fis = new FileInputStream(fileName);
            }
        }

        // Print the response message.
        System.out.println("\nRESPONSE:\n------------------------------");
        System.out.println(statusLine);
        System.out.println(contentTypeLine);
        if (entityBody != null) {
            System.out.println(entityBody);
        }
        
        // Send the status line.
        os.writeBytes(statusLine);
        // Send the content type line.
        os.writeBytes(contentTypeLine);
        // Send a blank line to indicate the end of the header lines.
        os.writeBytes(CRLF);
        
        // Send the entity body.

        // Code for Task 1
        // if (fileExists) {
        //     sendBytes(fis, os);
        //     fis.close();
        // } else {
        //     os.writeBytes(entityBody);
        // }

        // Code for Task 2
        if (fileExists) {
            sendBytes(fis, os);
            fis.close();
        } else {
            if (!contentType(fileName).equalsIgnoreCase("text/plain")) {
                os.writeBytes(entityBody);
            } else {
                sendBytes(fis, os);
            }
        }
    
        // TESTING INSTRUCTIONS *NOT FOR GRADERS*

        // use: http://127.0.0.1:6789/<file.ext>
        // this will give the response for html, jpg, and gif

        // use: http://127.0.0.1:6789
        // this will give a 404 and the Not Found page
    }

}