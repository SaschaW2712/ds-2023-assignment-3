import java.util.ArrayList;
import java.util.List;

import enums.Regularity;
import enums.ResponseLikelihood;

public class Acceptor {
    int memberId;
    MemberResponsiveness responsiveness;

    int promisedProposalNumber = -1;
    Proposal acceptedProposal = null;
    
    public Acceptor(int memberId) {
        this.memberId = memberId;

        List<LocationWithProbability> memberLocations = new ArrayList<LocationWithProbability>();
        
        switch (memberId) {
            case 1:
                Location member1HomeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
                memberLocations.add(new LocationWithProbability(member1HomeLocation, Regularity.Always));
                break;
            case 2:
                Location hillsLocation = new Location(InternetSpeed.Low, ResponseLikelihood.Improbable);
                Location cafeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
                memberLocations.add(new LocationWithProbability(hillsLocation, Regularity.Often));
                memberLocations.add(new LocationWithProbability(cafeLocation, Regularity.Rarely));
                break;
            case 3:
                Location member3HomeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
                Location coorongLocation = new Location(InternetSpeed.Low, ResponseLikelihood.Impossible);
                memberLocations.add(new LocationWithProbability(member3HomeLocation, Regularity.Often));
                memberLocations.add(new LocationWithProbability(coorongLocation, Regularity.Rarely));
                break;
            default:
                Location otherMembersHomeLocation = new Location(InternetSpeed.High, ResponseLikelihood.Certain);
                Location workLocation = new Location(InternetSpeed.Medium, ResponseLikelihood.Impossible);
                memberLocations.add(new LocationWithProbability(otherMembersHomeLocation, Regularity.Sometimes));
                memberLocations.add(new LocationWithProbability(workLocation, Regularity.Sometimes));
        }

        this.responsiveness = new MemberResponsiveness(memberLocations);
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