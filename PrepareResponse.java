public class PrepareResponse {
    int id;
    Proposal acceptedProposal;

    public PrepareResponse(int id) {
        this.id = id;
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
