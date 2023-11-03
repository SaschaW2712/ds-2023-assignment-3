import java.io.*;
import java.net.*;

import dataclasses.Proposal;
import dataclasses.ResponseWithOptionalProposal;


public class Proposer {
    public PrintStream outputStream = new PrintStream(System.out);

    int memberId;
    int proposalNumber;
    int numAcceptors;
    
    Socket socket;
    
    public Proposer(int memberId, int numAcceptors) {
        this.memberId = memberId;
        
        //ensure uniqueness (proposal number is always incremented by the same amount, so they interleave between members)
        this.proposalNumber = -3 + memberId;
        
        this.numAcceptors = numAcceptors;
    }

    public Proposer(int memberId, int numAcceptors, PrintStream outputStream) {
        this.memberId = memberId;
        
        //ensure uniqueness (proposal number is always incremented by the same amount, so they interleave between members)
        this.proposalNumber = -3 + memberId;
        
        this.numAcceptors = numAcceptors;

        this.outputStream = outputStream;
    }
    
    public String propose(String value) {
        //increment by three for three proposers
        proposalNumber += 3;
        
        int promiseCount = 0;
        String proposedValue = null;
        int proposedValueProposalId = -1;
        
        int neededMajority = (numAcceptors / 2) + 1;
        
        try {
            socket = new Socket("localhost", 4567);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("Proposer " + memberId + " Prepare " + proposalNumber);
            
            boolean allAcceptorsResponded = false;
            
            String line;
            while (!socket.isClosed()
            && in != null
            && (line = in.readLine()) != null
            && !allAcceptorsResponded
            ) {                
                if (line.startsWith("TIMEOUT")) {
                    break;
                } else if (line.startsWith("MAJORITY")) {
                    break;
                }
                
                ResponseWithOptionalProposal result = parseAcceptorResponse(line);
                
                if (result != null) {
                    promiseCount++;
                    
                    if (result.proposal != null && result.proposal.proposalNumber > proposedValueProposalId) {
                        proposedValue = result.proposal.value;
                        proposedValueProposalId = result.proposal.proposalNumber;
                    }
                }
            }            
            socket.close();
        } catch (IOException e) {
            return "ENDED";
        }
        
        String acceptedValue;
        if (proposedValue == null) {
            acceptedValue = Integer.toString(memberId);
        } else {
            acceptedValue = proposedValue;
        }
        
        //Only send accept requests if we got majority on propose responses
        if (promiseCount >= neededMajority) {
            outputStream.println("M" + memberId + "'s proposal got prepare majority for value " + acceptedValue);
            
            int acceptCount = 0;
            
            try {
                Socket socket = new Socket("localhost", 4567);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.println("Proposer " + memberId + " Accept " + proposalNumber + " " + acceptedValue);
                
                boolean allAcceptorsResponded = false;
                String line;
                
                while ((line = in.readLine()) != null
                && !allAcceptorsResponded
                ) { 
                    if (line.startsWith("TIMEOUT")) {
                        break;
                    } else if (line.startsWith("MAJORITY")) {
                        allAcceptorsResponded = true;
                    } else if (line.startsWith("ENDED")) {
                        return "ENDED";
                    }
                    
                    ResponseWithOptionalProposal result = parseAcceptorResponse(line);
                    
                    if (result != null) {
                        acceptCount++;                        
                        
                        if (result.proposal != null) {
                            acceptedValue = result.proposal.value;
                        }
                    }
                }
                
                socket.close();
            } catch (IOException e) {
                return "ENDED";
            }
            
            if (acceptCount >= (numAcceptors / 2) + 1) {
                outputStream.println("M" + memberId + "'s proposal got accept majority for value " + acceptedValue);
                return "SUCCESS " + acceptedValue;
            }
            
            outputStream.println("M" + memberId + "'s proposal did not get accept majority, trying again.");
            return "FAILURE";
        }
        
        outputStream.println("M" + memberId + "'s proposal did not get accept majority, trying again.");
        return "FAILURE";
    }
    
    
    //Response will be of format "OK", "REJECTED", or "OK <previously accepted proposal ID> <previously accepted proposal value>"
    public ResponseWithOptionalProposal parseAcceptorResponse(String line) {
        if (line.contains("OK")) {
            String[] responseParams = line.split("\\s+");
            
            //Regular okay, no previously accepted proposal
            if (responseParams.length < 4) {
                return new ResponseWithOptionalProposal();
            } else {
                int acceptedProposalId = Integer.parseInt(responseParams[2]);
                String acceptedProposalValue = responseParams[3];
                return new ResponseWithOptionalProposal(new Proposal(acceptedProposalId, acceptedProposalValue));
            }
        }
        
        return null;
    }
    
    public void finish() {        
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}