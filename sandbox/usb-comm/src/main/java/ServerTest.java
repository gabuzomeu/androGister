import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerTest {

    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static BufferedReader bufferedReader;
    private static String inputLine;

    public static void main(String[] args) {
        // Wait for client to connect on 63400
        try {
            serverSocket = new ServerSocket(9100);
            clientSocket = serverSocket.accept();

          //  readServerLine(clientSocket.getInputStream());
           readServer(clientSocket.getInputStream());

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static void readServerLine(InputStream in) throws  IOException{
         bufferedReader = new BufferedReader(new InputStreamReader(in));
        // Get the client message
         while ((inputLine = bufferedReader.readLine()) != null)
            System.out.println(inputLine);
    }

    private static void readServer(InputStream in) throws  IOException{
        InputStreamReader input = new InputStreamReader(in);

        char[] buffer = new char[1]; // Adjust if you want
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1)
        {
            System.out.println("Char : [" + new String(buffer) + "] => " +    Character.codePointAt(buffer, 0));
         }

    }

}