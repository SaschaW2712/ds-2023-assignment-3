import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/*
 * Todo:
 *      - Refactor prepare responses to allow for "no response", "negative response", and "positive response"
 *      - Make Acceptor also use socket communication
 *      - Make members use the same socket for their accept and propose
 *      - Automated testing
 */

enum RequestType {
    Proposer,
    Acceptor
}

enum RequestPhase {
    Prepare,
    Accept
}

public class PaxosServer {

    private static List<Acceptor> acceptors = new ArrayList<>();
    private static List<Proposer> proposers = new ArrayList<>();
    private static ServerSocket serverSocket;
    private static String electionWinner;

    public static void main(String[] args) {
        runServerThread();

        // closeServer(); //TODO: run this when voting done somehow
    }
    
    public static void runServerThread() {
        Thread serverThread = new Thread(() -> {
            runServer();
        });
        
        serverThread.start();
    }

    public static void runServer() {
        try {
            serverSocket = new ServerSocket(4567);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                parseConnection(in, out);

                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Server socket has closed, voting is done.");
            System.out.println("Elected member: " + electionWinner);
        }
    }

    public static void closeServer() {
        System.out.println("Closing Paxos server socket");
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("IOException closing server socket");
            e.printStackTrace();
        }
    }

    public static void parseConnection(
        BufferedReader in,
        PrintWriter out
    ) {
        try {
            String[] input = in.readLine().split("\\s+");

            RequestType requestType = RequestType.valueOf(input[0]);
            int memberId = Integer.parseInt(input[1]);
            RequestPhase requestPhase = RequestPhase.valueOf(input[2]);
            int proposalNumber = Integer.parseInt(input[3]);

            switch (requestType) {
                case Proposer:
                    switch (requestPhase) {
                        case Prepare:
                            handlePrepareRequest(memberId, proposalNumber, out);
                            break;

                        case Accept:
                            String value = input[4];
                            handleAcceptRequest(memberId, proposalNumber, value, out);
                            break;
                    }
                    break;
                
                case Acceptor:
                    System.out.println("acceptor");
                    break;
            }
        } catch (Exception e) {
            System.out.println("Error in parseConnection: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
    
    //Handle a request from the proposer with id `memberId`, and proposal number `proposalNumber`
    public static void handlePrepareRequest(
        int memberId,
        int proposalNumber,
        PrintWriter out
    ) {
        System.out.println("PREPARE REQUEST: memberId " + memberId + ", proposal number " + proposalNumber);

        for (Acceptor acceptor: acceptors) {
            //TODO: reimplement with communication to acceptor sockets
            // if (acceptor.memberId != memberId) {
            //     PrepareResponse prepareResponse = acceptor.prepare(proposalNumber);

            //     if (prepareResponse == null || !prepareResponse.memberResponds) {
            //         System.out.println("PREPARE RESPONSE: acceptor " + acceptor.memberId + ", response REJECTED");
            //         out.println("REJECTED");
            //     } else {

            //         //If acceptedProposal is null, this is the first proposal we've seen so we like this value
            //         if (prepareResponse.acceptedProposal == null) {
            //             System.out.println("PREPARE RESPONSE: acceptor " + acceptor.memberId + ", response OK");
            //             out.println("OK");

            //         //If acceptedProposal is not null, this is the latest proposal we've seen but we've accepted a previous value already
            //         } else {
            //             System.out.println("PREPARE RESPONSE: acceptor " + acceptor.memberId + ", response OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
            //             out.println("OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
            //         }
            //     } 
            // }
        }
    }
    
    public static void handleAcceptRequest(
        int memberId,
        int proposalNumber,
        String value,
        PrintWriter out
    ) {
        System.out.println("ACCEPT REQUEST: memberId " + memberId + ", proposal number " + proposalNumber + ", value " + value);

        for (Acceptor acceptor: acceptors) {
            //TODO: reimplement with communication to acceptor sockets
            // if (acceptor.memberId != memberId) {
            //     boolean acceptResponse = acceptor.accept(proposalNumber, value);

            //     //If acceptResponse is true, it's accepted
            //     if (acceptResponse) {
            //             System.out.println("ACCEPT RESPONSE: acceptor " + acceptor.memberId + ", response OK");
            //             out.println("OK");

            //     //If acceptResponse is null or false, it's rejected
            //     } else {
            //         System.out.println("ACCEPT RESPONSE: acceptor " + acceptor.memberId + ", response REJECTED");
            //         out.println("REJECTED");
            //     }
            // }
        }
    }
}