package dataclasses;
public class PrepareResponse {
    public int id;
    public boolean memberResponds = true;
    public Proposal acceptedProposal;

    public PrepareResponse(int id) {
        this.id = id;
    }

    public PrepareResponse(int id, boolean memberResponds) {
        this.id = id;
        this.memberResponds = memberResponds;
    }

    public PrepareResponse(int id, int acceptedId, String acceptedValue) {
        this.id = id;
        this.acceptedProposal = new Proposal(acceptedId, acceptedValue);
    }

    public PrepareResponse(int id, Proposal acceptedProposal) {
        this.id = id;
        this.acceptedProposal = acceptedProposal;
    }
}
