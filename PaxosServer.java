import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
/*
* Todo:
*      - Refactor prepare responses to allow for "no response", "negative response", and "positive response"
*      - Make Acceptor also use socket communication
*      - Make members use the same socket for their accept and propose
*      - Automated testing
*/

import enums.RequestPhase;
import enums.ConnectionSource;

public class PaxosServer {
    
    public static List<Integer> currentProposers = new ArrayList<>();
    public static List<Integer> currentAcceptors = new ArrayList<>();
    public static ServerSocket serverSocket;
    
    public static Map<Integer, LinkedBlockingQueue<String>> proposerToAcceptorMessageQueues = new HashMap<Integer, LinkedBlockingQueue<String>>(); // Shared message queue for acceptors^
    public static Map<Integer, LinkedBlockingQueue<String>> acceptorToProposerMessageQueues = new HashMap<Integer, LinkedBlockingQueue<String>>(); // Shared queue for acceptor responses
    
    public static void main(String[] args) {
        runServer();

        // closeServer(); //TODO: run this when voting done somehow
    }
    
    // public static void runServerThread() {
    //     Thread serverThread = new Thread(() -> {
    //     });
        
    //     serverThread.start();
    // }
    
    public static void runServer() {
        try {
            serverSocket = new ServerSocket(4567);
            System.out.println("Paxos server started on port 4567");
            
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                parseConnection(socket, in, out);    
            }
        } catch (IOException e) {
            System.out.println("Server socket has closed, voting is done.");
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
    Socket socket,
    BufferedReader in,
    PrintWriter out
    ) {
        try {
            String line;
            if ((line = in.readLine()) != null) {
                System.out.println("Incoming connection: " + line);
                
                String[] input = line.split("\\s+");
                
                ConnectionSource connectionSource = ConnectionSource.valueOf(input[0]);
                int memberId = Integer.parseInt(input[1]);
                
                if (connectionSource == ConnectionSource.Proposer) {
                    RequestPhase requestPhase = RequestPhase.valueOf(input[2]);
                    int proposalNumber = Integer.parseInt(input[3]);
                    
                    String value;
                    if (input.length > 4) {
                        value = input[4];
                    } else {
                        value = null;
                    }
                    
                    handleProposerConnection(socket, requestPhase, memberId, proposalNumber, value);
                    
                } else if (connectionSource == ConnectionSource.Acceptor) {
                    handleAcceptorConnection(socket, memberId);
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error in parseConnection: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
    
    public static void handleProposerConnection(
    Socket socket,
    RequestPhase requestPhase,
    int memberId,
    int proposalNumber,
    String value
    ) {
        if (!currentProposers.contains(memberId)) {
            currentProposers.add(memberId);
            proposerToAcceptorMessageQueues.put(memberId - 1, new LinkedBlockingQueue<>());
            acceptorToProposerMessageQueues.put(memberId - 1, new LinkedBlockingQueue<>());

            System.out.println("Proposer " + memberId + " added to current proposers. New size: Acceptors " + acceptorToProposerMessageQueues.size() + ", Proposers " + proposerToAcceptorMessageQueues.size());
        }
        
        switch (requestPhase) {
            case Prepare:
            handlePrepareRequest(socket, memberId, proposalNumber);
            break;
            
            case Accept:
            handleAcceptRequest(socket, memberId, proposalNumber, value);
            break;
        }
    }
    
    public static void handleAcceptorConnection(
    Socket socket,
    int memberId
    ) {
        if (!currentAcceptors.contains(memberId)) {
            currentAcceptors.add(memberId);
            proposerToAcceptorMessageQueues.put(memberId - 1, new LinkedBlockingQueue<>());
            acceptorToProposerMessageQueues.put(memberId - 1, new LinkedBlockingQueue<>());
        }
        
        System.out.println("Acceptor " + memberId + " added to listening acceptors. New size: Acceptors " + acceptorToProposerMessageQueues.size() + ", Proposers " + proposerToAcceptorMessageQueues.size());

        new Thread(() -> {        
            try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                while (!socket.isClosed()) {
                    
                    // System.out.println("Current sizes of queues: proposer " + proposerToAcceptorMessageQueues.size() + ", acceptors: " + acceptorToProposerMessageQueues.size());
                    String message = proposerToAcceptorMessageQueues.get(memberId - 1).poll();
                    
                    if (message != null) {
                        System.out.println("Acceptor " + memberId + " polled proposerMessageQueue successfully: " + message);
                        out.println(message);
                        
                        int proposerMemberId = Integer.parseInt(message.split("\\s+")[1]);
                        String line;
                        if ((line = in.readLine()) != null) {
                            System.out.println("Acceptor " + memberId + " response: " + line);
                            
                            acceptorToProposerMessageQueues.get(proposerMemberId - 1).put(line);
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Error in handleAcceptorConnection");
                e.printStackTrace();
            }
        }).start();
        
    }
    
    //Handle a request from the proposer with id `memberId`, and proposal number `proposalNumber`
    public static void handlePrepareRequest(
    Socket socket,
    int memberId,
    int proposalNumber
    ) {
        System.out.println("PREPARE REQUEST: memberId " + memberId + ", proposal number " + proposalNumber);
        
        try(PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            for (Map.Entry<Integer, LinkedBlockingQueue<String>> pQueueEntry : proposerToAcceptorMessageQueues.entrySet()) {
                pQueueEntry.getValue().put("Proposer " + memberId + " Prepare " + proposalNumber);
            }
            
            int numResponses = 0;
            
            while (numResponses < currentAcceptors.size()) {
                String line;
                if ((line = acceptorToProposerMessageQueues.get(memberId - 1).poll()) != null) {
                    System.out.println("Proposer " + memberId + " polled acceptorResponseQueue successfully: " + line);
                    out.println(line);
                    numResponses++;
                }
            }
            
            socket.shutdownOutput();
            
        } catch (InterruptedException | IOException e) {
            System.out.println("Exception in handlePrepareRequest for memberId " + memberId + ": " + e.getLocalizedMessage());
            System.out.println("Current sizes of queues: proposer " + proposerToAcceptorMessageQueues.size() + ", acceptor: " + acceptorToProposerMessageQueues.size());
            e.printStackTrace();
        }
    }
    
    public static void handleAcceptRequest(
    Socket socket,
    int memberId,
    int proposalNumber,
    String value
    ) {
        System.out.println("ACCEPT REQUEST: memberId " + memberId + ", proposal number " + proposalNumber + ", value " + value);
        
        try(PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            for (Map.Entry<Integer, LinkedBlockingQueue<String>> pQueueEntry : proposerToAcceptorMessageQueues.entrySet()) {
                pQueueEntry.getValue().put("Proposer " + memberId + " Accept " + proposalNumber + " " + value);
            }
            
            int numResponses = 0;
            
            while (numResponses < currentAcceptors.size()) {
                String line;
                if ((line = acceptorToProposerMessageQueues.get(memberId - 1).poll()) != null) {
                    System.out.println("Proposer " + memberId + " polled acceptorResponseQueue successfully: " + line);
                    out.println(line);
                    numResponses++;
                }
            }
            
            socket.shutdownOutput();
            
        } catch (InterruptedException | IOException e) {
            System.out.println("Exception in handlePrepareRequest: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}