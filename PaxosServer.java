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

    public static List<Acceptor> acceptors = new ArrayList<>();
    public static List<Proposer> proposers = new ArrayList<>();

    public static void main(String[] args) {
        for (int memberId = 0; memberId < 9; memberId++) {
            acceptors.add(new Acceptor(memberId));
        }
        
        for (int memberId = 0; memberId < 3; memberId++) {
            List<Acceptor> acceptorsWithoutCurrentMember = acceptors;
            acceptorsWithoutCurrentMember.remove(memberId);
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
                        handlePrepareRequest(memberId, proposalNumber, out);
                        break;

                    case Accept:
                        int value = Integer.parseInt(input[4]);
                        handleAcceptRequest(memberId, proposalNumber, value);
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

        //TODO: handle overall result of proposal
    }
    
    //Handle a request from the proposer with id `memberId`, and proposal number `proposalNumber`
    public static void handlePrepareRequest(
        int memberId,
        int proposalNumber,
        PrintWriter out
    ) {
        System.out.println("PREPARE REQUEST: member " + memberId + ", proposal number " + proposalNumber);

        for (Acceptor acceptor: acceptors) {
            if (acceptor.memberId != memberId) {
                PrepareResponse prepareResponse = acceptor.prepare(proposalNumber);

                //If prepareResponse is not null, this is the latest proposal we've seen
                if (prepareResponse != null) {

                    //If acceptedProposal is null, this is the first proposal we've seen so we like this value
                    if (prepareResponse.acceptedProposal == null) {
                        System.out.println("PREPARE RESPONSE: acceptor " + acceptor.memberId + ", response OK");
                        out.println("OK");

                    //If acceptedProposal is not null, this is the latest proposal we've seen but we've accepted a previous value already
                    } else {
                        System.out.println("PREPARE RESPONSE: acceptor " + acceptor.memberId + ", response OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
                        out.println("OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
                    }

                //If prepareResponse is null, this proposal is older than the latest we've seen so we ignore it
                } else {
                    System.out.println("PREPARE RESPONSE: acceptor " + acceptor.memberId + ", response IGNORED");
                    out.println("IGNORED");
                }
            }
        }
    }
    
    public static void handleAcceptRequest(
        int memberId,
        int proposalNumber,
        int value
    ) {
        //TODO: handle   
    }
}
