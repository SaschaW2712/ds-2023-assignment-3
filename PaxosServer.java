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
*      - Fix timeouts causing hangs
*      - Automated testing
*/
import java.util.concurrent.TimeUnit;

import enums.RequestPhase;
import enums.ConnectionSource;

public class PaxosServer {
    
    public static List<Integer> currentProposers = new ArrayList<>();
    public static List<Integer> currentAcceptors = new ArrayList<>();
    public static ServerSocket serverSocket;
    
    public static Map<Integer, LinkedBlockingQueue<String>> proposerToAcceptorMessageQueues = new HashMap<Integer, LinkedBlockingQueue<String>>(); // Shared message queue for acceptors
    public static Map<Integer, LinkedBlockingQueue<String>> acceptorToProposerMessageQueues = new HashMap<Integer, LinkedBlockingQueue<String>>(); // Shared queue for acceptor responses
    
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
        if (!currentProposers.contains(memberId)) {
            currentProposers.add(memberId);
            
            if (!proposerToAcceptorMessageQueues.containsKey(memberId - 1)) {
                proposerToAcceptorMessageQueues.put(memberId - 1, new LinkedBlockingQueue<>());
            }
            
            if (!acceptorToProposerMessageQueues.containsKey(memberId - 1)) {
                acceptorToProposerMessageQueues.put(memberId - 1, new LinkedBlockingQueue<>());
            }
            
            System.out.println("Proposer " + memberId + " added to current proposers. New size: Acceptors " + acceptorToProposerMessageQueues.size() + ", Proposers " + proposerToAcceptorMessageQueues.size());
        }
        
        switch (requestPhase) {
            case Prepare:
            handlePrepareRequest(socket, out, memberId, proposalNumber);
            break;
            
            case Accept:
            handleAcceptRequest(socket, out, memberId, proposalNumber, value);
            break;
        }
    }
    
    public static void handleAcceptorConnection(
    Socket socket,
    BufferedReader in,
    PrintWriter out,
    int memberId
    ) {
        
        if (!currentAcceptors.contains(memberId)) {
            currentAcceptors.add(memberId);
            proposerToAcceptorMessageQueues.put(memberId - 1, new LinkedBlockingQueue<>());
        }
        
        System.out.println("Acceptor " + memberId + " added to listening acceptors. New size: Acceptors " + acceptorToProposerMessageQueues.size() + ", Proposers " + proposerToAcceptorMessageQueues.size());
        
        try {
            while (!socket.isClosed()) {
                LinkedBlockingQueue<String> proposerQueue = proposerToAcceptorMessageQueues.get(memberId - 1);
                LinkedBlockingQueue<String> acceptorQueue = acceptorToProposerMessageQueues.get(memberId - 1);
                if (proposerQueue != null) {
                    
                    //Poll the queue for my memberId, to get messages from all proposers to me
                    String message = proposerQueue.poll();
                    if (message != null) {
                        System.out.println("Acceptor " + memberId + " polled proposerMessageQueue successfully: " + message);
                        out.println(message);
                        
                        int proposerMemberId = Integer.parseInt(message.split("\\s+")[1]);
                        String line;
                        if ((line = in.readLine()) != null) {
                            System.out.println("Acceptor " + memberId + " response: " + line);
                            
                            if (acceptorQueue != null) {
                                acceptorQueue.put(line);
                            }
                        }
                    }
                }
            }
            // while (!socket.isClosed()) {
                
                //     // System.out.println("Current sizes of queues: proposer " + proposerToAcceptorMessageQueues.size() + ", acceptors: " + acceptorToProposerMessageQueues.size());
                //     String message = proposerToAcceptorMessageQueues.get(memberId - 1).poll();
                
                //     if (message != null) {
                    //         System.out.println("Acceptor " + memberId + " polled proposerMessageQueue successfully: " + message);
                    //         out.println(message);
                    
                    //         int proposerMemberId = Integer.parseInt(message.split("\\s+")[1]);
                    //         String line;
                    //         if ((line = in.readLine()) != null) {
                        //             System.out.println("Acceptor " + memberId + " response: " + line);
                        
                        //             acceptorToProposerMessageQueues.get(proposerMemberId - 1).put(line);
                        //         }
                        //     }
                        // }
                    } catch (IOException | InterruptedException e) {
                        System.out.println("Error in handleAcceptorConnection");
                        e.printStackTrace();
                    }        
                }
                
                //Handle a request from the proposer with id `memberId`, and proposal number `proposalNumber`
                public static void handlePrepareRequest(
                Socket socket,
                PrintWriter out,
                int memberId,
                int proposalNumber
                ) {
                    System.out.println("PREPARE REQUEST: memberId " + memberId + ", proposal number " + proposalNumber);
                    
                    // new Thread(() -> {
                        
                        try {
                            for (Map.Entry<Integer, LinkedBlockingQueue<String>> pQueueEntry : proposerToAcceptorMessageQueues.entrySet()) {
                                pQueueEntry.getValue().put("Proposer " + memberId + " Prepare " + proposalNumber);
                            }
                            
                            int successfulResponses = 0;
                            int unsuccessfulResponses = 0;
                            
                            while ((successfulResponses + unsuccessfulResponses) < currentAcceptors.size()) {
                                //Wait up to 15 seconds for response
                                //Poll my own queue, which acceptors should be writing to
                                String line = acceptorToProposerMessageQueues.get(memberId - 1).poll(3, TimeUnit.SECONDS);
                                if (line != null) {
                                    System.out.println("Proposer " + memberId + " polled acceptorResponseQueue successfully: " + line);
                                    out.println(line);
                                    successfulResponses++;
                                } else {
                                    System.out.println("Proposer " + memberId + " received no response from acceptor");
                                    out.println("NORESPONSE");
                                    unsuccessfulResponses++;
                                }
                                
                                System.out.println("(Prepare " + memberId + " " + proposalNumber + ") Successful: " + successfulResponses + ", unsuccessful: " + unsuccessfulResponses);
                            }
                            
                            
                            socket.shutdownOutput();
                            System.out.println("(Prepare " + memberId + " " + proposalNumber + ") Loop done, closed socket output");
                            
                            
                        } catch (InterruptedException | IOException e) {
                            System.out.println("Exception in handlePrepareRequest for memberId " + memberId + ": " + e.getLocalizedMessage());
                            System.out.println("Current sizes of queues: proposer " + proposerToAcceptorMessageQueues.size() + ", acceptor: " + acceptorToProposerMessageQueues.size());
                            e.printStackTrace();
                        }
                        // }).start();
                    }
                    
                    public static void handleAcceptRequest(
                    Socket socket,
                    PrintWriter out,
                    int memberId,
                    int proposalNumber,
                    String value
                    ) {
                        System.out.println("ACCEPT REQUEST: memberId " + memberId + ", proposal number " + proposalNumber + ", value " + value);
                        
                        // new Thread(() -> {
                            try {
                                for (Map.Entry<Integer, LinkedBlockingQueue<String>> pQueueEntry : proposerToAcceptorMessageQueues.entrySet()) {
                                    pQueueEntry.getValue().put("Proposer " + memberId + " Accept " + proposalNumber + " " + value);
                                }
                                
                                int successfulResponses = 0;
                                int unsuccessfulResponses = 0;
                                
                                while ((successfulResponses + unsuccessfulResponses) < currentAcceptors.size()) {
                                    //Wait up to 15 seconds for response
                                    String line = acceptorToProposerMessageQueues.get(memberId - 1).poll(2, TimeUnit.SECONDS);
                                    if (line != null) {
                                        System.out.println("Proposer " + memberId + " polled acceptorResponseQueue successfully: " + line);
                                        out.println(line);
                                        successfulResponses++;
                                    } else {
                                        System.out.println("Proposer " + memberId + " received no response from acceptor");
                                        out.println("NORESPONSE");
                                        unsuccessfulResponses++;
                                    }
                                }
                                
                                socket.shutdownOutput();
                                
                            } catch (InterruptedException | IOException e) {
                                System.out.println("Exception in handleAcceptRequest for memberId " + memberId + ": " + e.getLocalizedMessage());
                                e.printStackTrace();
                            }
                            // }).start();
                        }
                    }