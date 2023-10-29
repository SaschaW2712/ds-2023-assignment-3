import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PaxosServer {
    public static void main(String[] args) {
        List<Acceptor> acceptors = new ArrayList<>();
        for (int memberId = 0; memberId < 9; memberId++) {
            acceptors.add(new Acceptor(memberId));
        }
        
        List<Proposer> proposers = new ArrayList<>();
        for (int memberId = 0; memberId < 3; memberId++) {
            proposers.add(new Proposer(memberId, acceptors));
        }
        
        try {
            ServerSocket serverSocket = new ServerSocket(4567);
            
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                String input = in.readLine();
                if (input.startsWith("PREPARE")) {

                    // TODO: Handle PREPARE message

                } else if (input.startsWith("ACCEPT")) {

                    // TODO: Handle ACCEPT message

                }
                
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
