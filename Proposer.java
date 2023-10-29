import java.io.*;
import java.net.*;
import java.util.*;

public class Proposer {
    int memberId;
    int proposalNumber = 0;
    List<Acceptor> acceptors;
    
    public Proposer(int memberId, List<Acceptor> acceptors) {
        this.memberId = memberId;
        
        //ensure uniqueness (proposal number is always incremented by the same amount, so they interleave between members)
        this.proposalNumber += memberId;

        this.acceptors = acceptors; 
    }
    
    public String propose(String value) {
        proposalNumber += 3; //increment by three for three proposers
        int prepareCount = 0;
        String proposedValue = null;
        
        for (Acceptor acceptor : acceptors) {
            try {
                Socket socket = new Socket("localhost", 4567); // Replace with the appropriate IP and port
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.println("PREPARE " + proposalNumber);

                String response = in.readLine();

                if (response != null) {
                    prepareCount++;
                    if (proposedValue == null || response.compareTo(proposedValue) > 0) {
                        proposedValue = response;
                    }
                }
                
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        //Only send accept requests if we got majority on propose responses
        if (prepareCount >= (acceptors.size() / 2) + 1) {
            int acceptCount = 0;
            
            for (Acceptor acceptor : acceptors) {
                try {
                    Socket socket = new Socket("localhost", 4567); // Replace with the appropriate IP and port
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    
                    out.println("ACCEPT " + proposalNumber + " " + value);
                    String response = in.readLine();
                    if (response != null && response.equals("OK")) {
                        acceptCount++;
                    }
                    
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (acceptCount >= (acceptors.size() / 2) + 1) {
                return "SUCCESS";
            }
        }
        
        return "FAILURE";
    }
}