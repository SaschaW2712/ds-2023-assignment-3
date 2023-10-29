public class Acceptor {
    int memberId;
    int promisedProposalNumber = -1;
    Proposal acceptedProposal = null;
    
    public Acceptor(int memberId) {
        this.memberId = memberId;
    }
    
    public PrepareResponse prepare(int proposalNumber) {
        if (proposalNumber > promisedProposalNumber) {
            promisedProposalNumber = proposalNumber;
            return new PrepareResponse(proposalNumber);
        } else if (acceptedProposal != null) {
            return new PrepareResponse(proposalNumber, acceptedProposal);
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