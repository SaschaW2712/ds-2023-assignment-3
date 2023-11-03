import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/*
* Todo:
*      - Finish resolving completed election for case where threads exit before all proposers know the result
*      - Clean up output
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

                    if (line.startsWith("Election Result")) {
                        handleElectionResultRequest(socket);
                        return;
                    }
                    
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

    public static void handleElectionResultRequest(
        Socket socket
    ) {

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
            
            synchronized (PaxosServer.class) {
                updateProposerResponseCounts(memberId, true);
                System.out.println("Proposer " + memberId + " added to current proposers. Total proposers: " + proposerSockets.size());
            }
            
        }
        
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
            int majority = 5;
            

            //Add a small amount of randomness to timeout, to avoid repeated failed votes due to incrementing proposal numbers
            double timeout = 5000 + (3 * (Math.random() * 1000));
            
            while (
            proposerResponseCounts.get(memberId) < majority
            && (System.currentTimeMillis() - timeout) < startTimeMs //set timeout on waiting
            ) {
                //wait
            }
            
            if (proposerResponseCounts.get(memberId) < majority) {
                System.out.println("Proposer " + memberId + " ran out of time for acceptor responses");
                out.println("TIMEOUT");
            } else {
                System.out.println("Proposer " + memberId + " received responses from majority of acceptors");
                out.println("MAJORITY");
            }
            
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket");
                e.printStackTrace();
            }
            
            proposerSockets.put(memberId, null);
            
            synchronized (PaxosServer.class) {
                updateProposerResponseCounts(memberId, true);
            }
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
        
            try {
                while (!socket.isClosed()) {
                    String message = in.readLine();
                    
                    if (message != null) {
                        // System.out.println("Acceptor " + memberId + " response: " + message);
                        
                        String[] messageParts = message.split("\\s+");
                        
                        int proposerMemberId = Integer.parseInt(messageParts[0]);
                        
                        Socket proposerSocket = proposerSockets.get(proposerMemberId);
                        
                        synchronized (PaxosServer.class) {
                            if (proposerSocket != null && !socket.isClosed()) {
                                PrintWriter proposerOut = new PrintWriter(proposerSocket.getOutputStream(), true);
                                proposerOut.println(message);
                                
                                updateProposerResponseCounts(proposerMemberId, false);
                                System.out.println("New response count for proposer " + proposerMemberId + ": " + proposerResponseCounts.get(proposerMemberId));
                            } else {
                                System.out.println("(Acceptor " + memberId + ") Socket for member " + proposerMemberId + " is closed or null");
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                System.err.println("Acceptor cannot respond to proposer, their socket has been closed (usually due to getting a majority vote)");
            } catch (IOException e) {
                System.out.println("IOException in handleAcceptorConnection for memberId " + memberId + ": " + e.getLocalizedMessage());
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
    
    public static synchronized void updateProposerResponseCounts(int memberId, boolean setToZero) {
        int count = proposerResponseCounts.getOrDefault(memberId, -1);
        
        if (setToZero || count == -1) {
            proposerResponseCounts.put(memberId, 0);
        } else {
            proposerResponseCounts.put(memberId, count + 1);
        }
    }
}