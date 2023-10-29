public class ResponseWithOptionalProposal {
    Proposal proposal = null;
    
    ResponseWithOptionalProposal() { }
    
    ResponseWithOptionalProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}
