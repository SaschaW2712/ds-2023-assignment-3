import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

import dataclasses.Proposal;
import dataclasses.ResponseWithOptionalProposal;


public class Proposer {
    int memberId;
    int proposalNumber;
    int numAcceptors;
    
    public Proposer(int memberId, int numAcceptors) {
        this.memberId = memberId;
        
        //ensure uniqueness (proposal number is always incremented by the same amount, so they interleave between members)
        this.proposalNumber = -3 + memberId;
        
        this.numAcceptors = numAcceptors;
    }
    
    public String propose(String value) {
        //increment by three for three proposers
        //TODO: consider more random proposal numbers so winner varies
        proposalNumber += 3;
        
        int prepareCount = 0;
        String proposedValue = null;
        int proposedValueProposalId = -1;
        
        int neededMajority = (numAcceptors / 2) + 1;
        
        try {
            TimeUnit.SECONDS.sleep(2); //Give acceptors a change to connect before making proposals
        } catch (InterruptedException e) {
            System.out.println("InterruptedException in propose: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        
        try {
            Socket socket = new Socket("localhost", 4567);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("Proposer " + memberId + " Prepare " + proposalNumber);
            
            String line;
            while (prepareCount < numAcceptors && (line = in.readLine()) != null) {
                if (line.startsWith("NORESPONSE")) {
                    System.out.println("Proposer did not receive enough messages from Acceptors in 15 seconds, abandoning proposal.");
                    return "FAILURE";
                }

                // System.out.println("Proposer " + memberId + " received prepare response: " + line);
                ResponseWithOptionalProposal result = parseAcceptorResponse(line);
                
                if (result != null) {
                    prepareCount++;
                    
                    if (result.proposal != null && result.proposal.proposalNumber > proposedValueProposalId) {
                        proposedValue = result.proposal.value;
                        proposedValueProposalId = result.proposal.proposalNumber;
                    }
                }
            }
            
            System.out.println("Proposer " + memberId + " closing socket");
            
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String acceptedValue;
        if (proposedValue == null) {
            acceptedValue = Integer.toString(memberId);
        } else {
            acceptedValue = proposedValue;
        }
        
        //Only send accept requests if we got majority on propose responses
        if (prepareCount >= neededMajority) {
            System.out.println("\nProposer " + memberId + " got prepare majority\n");
            
            int acceptCount = 0;
            
            try {
                Socket socket = new Socket("localhost", 4567);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.println("Proposer " + memberId + " Accept " + proposalNumber + " " + acceptedValue);
                
                String line;
                while (acceptCount < neededMajority && (line = in.readLine()) != null) { 
                    if (line.startsWith("NORESPONSE")) {
                        System.out.println("Proposer did not receive enough messages from Acceptors in 15 seconds, abandoning proposal.");
                        return "FAILURE";
                    }
                    
                    ResponseWithOptionalProposal result = parseAcceptorResponse(line);
                    
                    if (result != null) {
                        acceptCount++;
                        // System.out.println("Proposer " + memberId + " received accept response: " + line + " (" + acceptCount + "/" + neededMajority + " accepts)");
                        
                        if (result.proposal != null) {
                            acceptedValue = result.proposal.value;
                        }
                    }
                }
                
                System.out.println("Proposer " + memberId + " closing socket");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            if (acceptCount >= (numAcceptors / 2) + 1) {
                return "SUCCESS " + acceptedValue;
            }
            
            System.out.println("\nProposer " + memberId + " did NOT GET accept majority\n");
            return "FAILURE";
        }
        
        System.out.println("\nProposer " + memberId + " did NOT GET prepare majority\n");
        return "FAILURE";
    }
    
    
    //Response will be of format "OK", "REJECTED", or "OK <previously accepted proposal ID> <previously accepted proposal value>"
    public ResponseWithOptionalProposal parseAcceptorResponse(String line) {
        if (line.startsWith("OK")) {
            String[] responseParams = line.split("\\s+");
            
            //Regular okay, no previously accepted proposal
            if (responseParams.length < 3) {
                return new ResponseWithOptionalProposal();
            } else {
                int acceptedProposalId = Integer.parseInt(responseParams[1]);
                String acceptedProposalValue = responseParams[2];
                return new ResponseWithOptionalProposal(new Proposal(acceptedProposalId, acceptedProposalValue));
            }
        }
        
        return null;
    }
}