package dataclasses;

public class ResponseWithOptionalProposal {
    public Proposal proposal = null;
    
    public ResponseWithOptionalProposal() { }
    
    public ResponseWithOptionalProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}
