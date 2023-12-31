import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;

import dataclasses.Location;
import dataclasses.MemberResponsiveness;
import dataclasses.PrepareResponse;
import dataclasses.Proposal;
import enums.RequestPhase;

public class Acceptor {
    public PrintStream outputStream = new PrintStream(System.out);

    int memberId;
    MemberResponsiveness responsiveness;
    boolean immediateResponse = false;
    
    int promisedProposalNumber = -1;
    Proposal acceptedProposal = null;
    
    Socket socket;
    PrintWriter out;
    BufferedReader in;

    int port = 4567;
    
    public Acceptor(int memberId, MemberResponsiveness responsiveness) {
        this.memberId = memberId;
        this.responsiveness = responsiveness;
    }
    
    public Acceptor(int memberId, MemberResponsiveness responsiveness, boolean immediateResponse, PrintStream outputStream, int port) {
        this.memberId = memberId;
        this.responsiveness = responsiveness;
        this.immediateResponse = immediateResponse;
        this.outputStream = outputStream;
        this.port = port;
    }
    
    public synchronized void listenToServer() {
        try {
            socket = new Socket("localhost", port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream.println("Acceptor M" + memberId + " connected to server");
            out.println("Acceptor " + memberId);
            
            handleServerResponse();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!socket.isClosed()) {
                closeSocket();
            }
        }
    }
    
    public void closeSocket() {
        
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
            outputStream.println("Acceptor M" + memberId + " closing socket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public synchronized void handleServerResponse() throws IOException {        
        
        String line;
        try {
            while (!socket.isClosed()
            && in != null 
            && (line = in.readLine()) != null
            ) {                
                
                String[] args = line.split("\\s+");
                
                int proposerMemberId = Integer.parseInt(args[1]);
                RequestPhase requestPhase = RequestPhase.valueOf(args[2]);
                int proposalNumber = Integer.parseInt(args[3]);
                
                if (requestPhase == RequestPhase.Prepare) {
                    
                    PrepareResponse prepareResponse = prepare(proposalNumber);
                    
                    if (prepareResponse == null) {
                        outputStream.println("PREPARE RESPONSE (M" + proposerMemberId + ", proposal " + proposalNumber + "): M" + memberId + " responds REJECTED");
                        out.println(proposerMemberId + " REJECTED");
                        
                    } else if (!prepareResponse.memberResponds) {
                        outputStream.println("PREPARE RESPONSE (M" + proposerMemberId + ", proposal " + proposalNumber + "): M" + memberId + " does not respond");
                        //No output, just disconnect
                        
                    } else if (prepareResponse.acceptedProposal == null) {
                        outputStream.println("PREPARE RESPONSE (M" + proposerMemberId + ", proposal " + proposalNumber + "): M" + memberId + " responds OK");
                        out.println(proposerMemberId + " OK");
                        
                    } else {
                        outputStream.println("PREPARE RESPONSE (M" + proposerMemberId + ", proposal " + proposalNumber + "): M" + memberId + " responds OK, with previously accepted proposal " + prepareResponse.acceptedProposal.proposalNumber + ", value " + prepareResponse.acceptedProposal.value);
                        out.println(proposerMemberId + " OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
                    }          
                    
                } else if (requestPhase == RequestPhase.Accept) {
                    String value = args[4];
                    boolean acceptResponse = accept(proposalNumber, value);
                    
                    //If acceptResponse is true, it's accepted
                    if (acceptResponse) {
                        outputStream.println("ACCEPT RESPONSE (M" + proposerMemberId + ", proposal " + proposalNumber + ", value " + value + "): M" + memberId + " responds OK");
                        out.println(proposerMemberId + " OK");
                        
                        //If acceptResponse is null or false, it's rejected
                    } else {
                        outputStream.println("ACCEPT RESPONSE (M" + proposerMemberId + ", proposal " + proposalNumber + ", value " + value + "): M" + memberId + " responds REJECTED");
                        out.println(proposerMemberId + " REJECTED");
                    }
                }
            }
        } catch (IOException e) {
            return;
        }
    }
    
    
    public PrepareResponse prepare(int proposalNumber) {
        
        if (!immediateResponse) {
            Location currentLocation = responsiveness.getMemberCurrentLocation();
            boolean respondToRequest = responsiveness.doesMemberRespond(currentLocation);
            
            if (respondToRequest == false) {
                return new PrepareResponse(proposalNumber, false);
            }
            
            try {
                responsiveness.delayResponse(currentLocation);
            } catch (InterruptedException e) {
                //If delay period is interrupted, send no response as if member never responded
                return new PrepareResponse(proposalNumber, false);
            }
        }
        
        
        if (proposalNumber > promisedProposalNumber) {
            promisedProposalNumber = proposalNumber;
            
            if (acceptedProposal != null) {
                return new PrepareResponse(proposalNumber, acceptedProposal);
            }
            
            return new PrepareResponse(proposalNumber);
        }
        
        return null;
    }
    
    public boolean accept(int proposalNumber, String value) {
        if (proposalNumber >= promisedProposalNumber) {
            promisedProposalNumber = proposalNumber;
            
            acceptedProposal = new Proposal(proposalNumber, value);
            return true;
        } else {
            return false;
        }
    }
}