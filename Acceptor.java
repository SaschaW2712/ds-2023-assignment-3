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
    
    public Acceptor(int memberId, MemberResponsiveness responsiveness) {
        this.memberId = memberId;
        this.responsiveness = responsiveness;
        this.exit = false;
    }
    
    public void listenToServer() {
        System.out.println("M" + memberId + " acceptor listening");

        while (true) {
            try {
                Socket socket = new Socket("localhost", 4567);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
                // System.out.println("Acceptor " + memberId + " connected to server");
                out.println("Acceptor " + memberId);
                handleServerResponse(in, out);
                
                // System.out.println("Acceptor " + memberId + " closing socket");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleServerResponse(
        BufferedReader in,
        PrintWriter out
    ) throws IOException {
        // System.out.println("Acceptor " + memberId + " handling server response");
        String line;
        
        while ((line = in.readLine()) != null) {
            if (line.startsWith("NORESPONSE")) {
                System.out.println("Acceptor received no messages in 15 seconds, re-connecting.");
                return;
            }

            System.out.println("Acceptor " + memberId + " got line: " + line);
            
            String[] args = line.split("\\s+");

            int proposerMemberId = Integer.parseInt(args[1]);
            RequestPhase requestPhase = RequestPhase.valueOf(args[2]);
            int proposalNumber = Integer.parseInt(args[3]);                
            
            if (requestPhase == RequestPhase.Prepare) {
                PrepareResponse prepareResponse = prepare(proposalNumber);
                
                if (prepareResponse == null || !prepareResponse.memberResponds) {
                    System.out.println("PREPARE RESPONSE (" + proposerMemberId + " " + proposalNumber + "): acceptor " + memberId + " does not respond");
                } else if (prepareResponse.acceptedProposal == null) {
                    System.out.println("PREPARE RESPONSE (" + proposerMemberId + " " + proposalNumber + "): acceptor " + memberId + ", response OK");
                    out.println("OK");
                } else {
                    System.out.println("PREPARE RESPONSE (" + proposerMemberId + " " + proposalNumber + "): acceptor " + memberId + ", response OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
                    out.println("OK " + prepareResponse.acceptedProposal.proposalNumber + " " + prepareResponse.acceptedProposal.value);
                }
            } else if (requestPhase == RequestPhase.Accept) {
                String value = args[4];
                boolean acceptResponse = accept(proposalNumber, value);
                
                //If acceptResponse is true, it's accepted
                if (acceptResponse) {
                    System.out.println("ACCEPT RESPONSE (" + proposerMemberId + " " + proposalNumber + " " + value + "): acceptor " + memberId + ", response OK");
                    out.println("OK");
                    
                //If acceptResponse is null or false, it's rejected
                } else {
                    System.out.println("ACCEPT RESPONSE (" + proposerMemberId + " " + proposalNumber + " " + value + "): acceptor " + memberId + ", response REJECTED");
                    out.println("REJECTED");
                }
            }
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