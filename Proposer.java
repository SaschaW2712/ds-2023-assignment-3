import java.io.*;
import java.net.*;
import java.util.*;

class ResponseWithOptionalProposal {
    Proposal proposal = null;

    ResponseWithOptionalProposal() { }

    ResponseWithOptionalProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}

public class Proposer {
    int memberId;
    int proposalNumber;
    List<Acceptor> acceptors;
    
    public Proposer(int memberId, List<Acceptor> acceptors) {
        this.memberId = memberId;
        
        //ensure uniqueness (proposal number is always incremented by the same amount, so they interleave between members)
        this.proposalNumber = -3 + memberId;

        this.acceptors = acceptors; 
    }
    
    public String propose(String value) {
        proposalNumber += 3; //increment by three for three proposers
        System.out.println("Proposal number: " + proposalNumber);
        int prepareCount = 0;
        String proposedValue = null;
        int proposedValueProposalId = -1;
        
        for (Acceptor acceptor : acceptors) {
            try {
                Socket socket = new Socket("localhost", 4567); // Replace with the appropriate IP and port
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.println("Proposer " + memberId + " Prepare " + proposalNumber);

                String response = in.readLine();
                ResponseWithOptionalProposal result = handleProposeResponse(response);

                if (result != null) {
                    prepareCount++;

                    if (result.proposal != null && result.proposal.proposalNumber > proposedValueProposalId) {
                        proposedValue = result.proposal.value;
                        proposedValueProposalId = result.proposal.proposalNumber;
                    }
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println();
        System.out.println("PREPARE response report: ");
        System.out.println("Member ID: " + memberId);
        System.out.println("Proposal number: " + proposalNumber);
        System.out.println("Prepare count: " + prepareCount);
        System.out.println("Proposed value: " + proposedValue);
        System.out.println();

        //Only send accept requests if we got majority on propose responses
        if (prepareCount >= (acceptors.size() / 2) + 1) {
            int acceptCount = 0;
            
            for (Acceptor acceptor : acceptors) {
                try {
                    Socket socket = new Socket("localhost", 4567); // Replace with the appropriate IP and port
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    
                    out.println("Proposer " + memberId + " Accept " + proposalNumber + " " + value);

                    String response = in.readLine();
                    if (response != null && response.equals("OK")) {
                        acceptCount++;
                    }
                    
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (acceptCount >= (acceptors.size() / 2) + 1) {
                return "Success";
            }
        }
        
        return "Failure";
    }

    //Response will be of format "OK", "IGNORED", or "OK <previously accepted proposal ID> <previously accepted proposal value>"
    public static ResponseWithOptionalProposal handleProposeResponse(String response) {
        if (response.startsWith("OK")) {
            String[] responseParams = response.split("\\s+");

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