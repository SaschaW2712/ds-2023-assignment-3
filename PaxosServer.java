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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
/*
* Todo:
*      - Finish implementing socket communication instead of message queues
*      - Fix timeouts causing hangs
*      - Automated testing
*/
import java.util.concurrent.TimeUnit;

import enums.RequestPhase;
import enums.ConnectionSource;

public class PaxosServer {
    
    public static ServerSocket serverSocket;
    
    public static Map<Integer, Socket> proposerSockets = new HashMap<>();
    public static Map<Integer, Socket> acceptorSockets = new HashMap<>();
    
    public static Map<Integer, Integer> proposerResponseCounts = new HashMap<>();
    
    public static ExecutorService threadPool = Executors.newCachedThreadPool();
    
    public static void main(String[] args) {
        runServer();
        
        // closeServer(); //TODO: run this when voting done somehow
    }
    
    
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
        threadPool.submit(() -> { // Submitting the task to the thread pool
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
                        
                        handleProposerConnection(socket, in, out, requestPhase, memberId, proposalNumber, value);
                        
                    } else if (connectionSource == ConnectionSource.Acceptor) {
                        handleAcceptorConnection(socket, in, out, memberId);
                    }
                }
                
            } catch (Exception e) {
                System.out.println("Error in parseConnection: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
    }
    
    public static void handleProposerConnection(
    Socket socket,
    BufferedReader in,
    PrintWriter out,
    RequestPhase requestPhase,
    int memberId,
    int proposalNumber,
    String value
    ) {
        if (!proposerSockets.containsKey(memberId) || proposerSockets.get(memberId) == null) {
            proposerSockets.put(memberId, socket);
            proposerResponseCounts.put(memberId, 0);
            
            System.out.println("Proposer " + memberId + " added to current proposers. Total proposers: " + proposerSockets.size());
        }
        
        threadPool.submit(() -> {
            switch (requestPhase) {
                case Prepare:
                handlePrepareRequest(socket, out, memberId, proposalNumber);
                break;
                
                case Accept:
                handleAcceptRequest(socket, out, memberId, proposalNumber, value);
                break;
            }
            
            long startTimeMs = System.currentTimeMillis();

            //MAJORITY CONSTANT
            int numAcceptors = 3;
            while (
                proposerResponseCounts.get(memberId) < numAcceptors
            && (System.currentTimeMillis() - 15000) < startTimeMs //set timeout on waiting
            ) {
                //wait
            }
            
            if (proposerResponseCounts.get(memberId) < numAcceptors) {
                System.out.println("Proposer " + memberId + " ran out of time for acceptor responses");
                out.println("TIMEOUT");
            } else {
                System.out.println("Proposer " + memberId + " received responses from all acceptors");
                out.println("FINISHED");
            }
            
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket");
                e.printStackTrace();
            }
            proposerSockets.put(memberId, null);
            proposerResponseCounts.put(memberId, 0);
            
        });
    }
    
    public static void handleAcceptorConnection(
    Socket socket,
    BufferedReader in,
    PrintWriter out,
    int memberId
    ) {
        if (!acceptorSockets.containsKey(memberId) || acceptorSockets.get(memberId) == null) {
            acceptorSockets.put(memberId, socket);
            
            System.out.println("Acceptor " + memberId + " added to listening acceptors. Total acceptors: " + acceptorSockets.size());
        }
        
        threadPool.submit(() -> {
            try {
                while (!socket.isClosed()) {
                    String message = in.readLine();
                    
                    if (message != null) {
                        System.out.println("Acceptor " + memberId + " response: " + message);
                        
                        String[] messageParts = message.split("\\s+");
                        
                        int proposerMemberId = Integer.parseInt(messageParts[0]);
                        
                        Socket proposerSocket = proposerSockets.get(proposerMemberId);
                        int proposerResponseCount = proposerResponseCounts.get(proposerMemberId);
                        
                        if (proposerSocket != null) {
                            PrintWriter proposerOut = new PrintWriter(proposerSocket.getOutputStream(), true);
                            proposerOut.println(message);

                            proposerResponseCounts.put(proposerMemberId, proposerResponseCount + 1);
                            System.out.println("New response count for proposer " + proposerMemberId + ": " + proposerResponseCounts.get(proposerMemberId));
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception in handleAcceptorConnection for memberId " + memberId + ": " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
    }
    
    //Handle a request from the proposer with id `memberId`, and proposal number `proposalNumber`
    public static void handlePrepareRequest(
    Socket socket,
    PrintWriter out,
    int memberId,
    int proposalNumber
    ) {
        System.out.println("PREPARE REQUEST: memberId " + memberId + ", proposal number " + proposalNumber);
        
        try {
            for (Socket acceptorSocket : acceptorSockets.values()) {
                PrintWriter acceptorOut = new PrintWriter(acceptorSocket.getOutputStream(), true);
                acceptorOut.println("Proposer " + memberId + " Prepare " + proposalNumber);
            }
            
        } catch (IOException e) {
            System.out.println("Exception in handlePrepareRequest for memberId " + memberId + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
    
    public static void handleAcceptRequest(
    Socket socket,
    PrintWriter out,
    int memberId,
    int proposalNumber,
    String value
    ) {
        System.out.println("ACCEPT REQUEST: memberId " + memberId + ", proposal number " + proposalNumber + ", value " + value);
        
        try {
            for (Socket acceptorSocket : acceptorSockets.values()) {
                PrintWriter acceptorOut = new PrintWriter(acceptorSocket.getOutputStream(), true);
                acceptorOut.println("Proposer " + memberId + " Accept " + proposalNumber + " " + value);
            }
        } catch (IOException e) {
            System.out.println("Exception in handleAcceptRequest for memberId " + memberId + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}