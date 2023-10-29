import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

enum NodeType {
    Proposer,
    Acceptor
}

enum RequestPhase {
    Prepare,
    Accept
}

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
        
        new Thread(() -> {
            runServer();
        }).start();
        
        for (Proposer proposer : proposers) {
            new Thread(() -> {
                runProposal(proposer);
            }).start();
        }
    }
    
    public static void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(4567)) {
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                parseConnection(in, out);

                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void parseConnection(
        BufferedReader in,
        PrintWriter out
    ) {
        try {
            String[] input = in.readLine().split("\\s+");

            NodeType nodeType = NodeType.valueOf(input[0]);
            int memberId = Integer.parseInt(input[1]);
            RequestPhase requestPhase = RequestPhase.valueOf(input[2]);
            int proposalNumber = Integer.parseInt(input[3]);

            if (nodeType == NodeType.Proposer) {

                switch (requestPhase) {
                    case Prepare:
                        handlePrepareResponse(memberId, proposalNumber);
                        break;

                    case Accept:
                        int value = Integer.parseInt(input[4]);
                        handleAcceptResponse(memberId, proposalNumber, value);
                        break;
                }

            } else {
                System.out.println("This is surprising");
                //TODO: handle error, or "Acceptor" connection
            }

        } catch (Exception e) {
            System.out.println("Error in parseConnection: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
    
    public static void runProposal(Proposer proposer) {
        String result = proposer.propose(Integer.toString(proposer.memberId));
        System.out.println("Result for member " + proposer.memberId + ": " + result);
    }
    
    public static void handlePrepareResponse(
        int memberId,
        int proposalNumber
    ) {
        
    }
    
    public static void handleAcceptResponse(
        int memberId,
        int proposalNumber,
        int value
    ) {
        
    }
}
