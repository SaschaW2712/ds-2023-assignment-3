public class Acceptor {
    int memberId;
    int promisedProposalNumber = -1;
    Proposal acceptedProposal = null;
    
    public Acceptor(int memberId) {
        this.memberId = memberId;
    }
    
    public String prepare(int proposalNumber) {
        if (proposalNumber > promisedProposalNumber) {
            promisedProposalNumber = proposalNumber;
            if (acceptedProposal != null) {
                return acceptedProposal.value;
            } else {
                return null;
            }
        } else {
            return null;
        }
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