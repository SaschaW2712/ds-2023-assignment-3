import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import dataclasses.Location;
import dataclasses.MemberResponsiveness;
import dataclasses.PrepareResponse;
import dataclasses.Proposal;
import enums.RequestPhase;

public class Acceptor {
    int memberId;
    MemberResponsiveness responsiveness;
    boolean exit;
    
    int promisedProposalNumber = -1;
    Proposal acceptedProposal = null;
    
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    
    public Acceptor(int memberId, MemberResponsiveness responsiveness) {
        this.memberId = memberId;
        this.responsiveness = responsiveness;
        this.exit = false;
    }
    
    public synchronized void listenToServer() {
        try {
            socket = new Socket("localhost", 4567);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Acceptor " + memberId + " connected to server");
            out.println("Acceptor " + memberId);
            
            while (true) {
                handleServerResponse();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }
    
    private void closeSocket() {
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
System.out.println("Acceptor " + memberId + " closing socket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public synchronized void handleServerResponse() throws IOException {        
System.out.println("Acceptor " + memberId + " handling server response");
        
        String line;
        try {
            while ((line = in.readLine()) != null) {                
System.out.println("Acceptor " + memberId + " got line: " + line);
                
                String[] args = line.split("\\s+");
                
                int proposerMemberId = Integer.parseInt(args[1]);
                RequestPhase requestPhase = RequestPhase.valueOf(args[2]);
                int proposalNumber = Integer.parseInt(args[3]);
                
                if (requestPhase == RequestPhase.Prepare) {
                    
                    PrepareResponse prepareResponse = prepare(proposalNumber);
                    
                    if (prepareResponse == null) {
                        out.println(proposerMemberId + " REJECTED");
                        
                    } else if (!prepareResponse.memberResponds) {
System.out.println("PREPARE RESPONSE (" + proposerMemberId + " " + proposalNumber + "): acceptor " + memberId + " does not respond");
                        //No output, just disconnect
                        
                    } else if (prepareResponse.acceptedProposal == null) {
System.out.println("PREPARE RESPONSE (" + proposerMemberId + " " + proposalNumber + "): acceptor " + memberId + ", response OK");
                        out.println(proposerMemberId + " OK");
                        
                    } else {
System.out.println("PREPARE RESPONSE (" + proposerMemberId + " " + proposalNumber + "): acceptor " + memberId + ", response OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
                        out.println(proposerMemberId + " OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
                    }          
                    
                } else if (requestPhase == RequestPhase.Accept) {
                    String value = args[4];
                    boolean acceptResponse = accept(proposalNumber, value);
                    
                    //If acceptResponse is true, it's accepted
                    if (acceptResponse) {
System.out.println("ACCEPT RESPONSE (" + proposerMemberId + " " + proposalNumber + " " + value + "): acceptor " + memberId + ", response OK");
                        out.println(proposerMemberId + " OK");
                        
                        //If acceptResponse is null or false, it's rejected
                    } else {
System.out.println("ACCEPT RESPONSE (" + proposerMemberId + " " + proposalNumber + " " + value + "): acceptor " + memberId + ", response REJECTED");
                        out.println(proposerMemberId + " REJECTED");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public PrepareResponse prepare(int proposalNumber) {
        
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
            
System.out.println("Setting accepted proposal");
            acceptedProposal = new Proposal(proposalNumber, value);
            return true;
        } else {
            return false;
        }
    }
}