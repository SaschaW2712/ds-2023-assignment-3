import java.util.List;

import enums.Regularity;
import enums.ResponseLikelihood;

enum InternetSpeed {
    High,
    Medium,
    Low
}

class Location {
    InternetSpeed internetSpeed;
    ResponseLikelihood responseLikelihood;

    public Location(InternetSpeed internetSpeed, ResponseLikelihood responseLikelihood) {
        this.internetSpeed = internetSpeed;
        this.responseLikelihood = responseLikelihood;
    }

    public boolean doesLocationRespond() {
        double p = responseLikelihood.value;

        double randomNumber = Math.random();

        return randomNumber < p;
    }
}

class LocationWithProbability {
    Location location;
    Regularity regularity;

    public LocationWithProbability(Location location, Regularity regularity) {
        this.location = location;
        this.regularity = regularity;
    }
}

public class MemberResponsiveness {
    List<LocationWithProbability> possibleLocations;

    public MemberResponsiveness(List<LocationWithProbability> locations) {
        this.possibleLocations = locations;
    }
    
    public boolean doesMemberRespond() {
        Location currentLocation = getMemberCurrentLocation();

        return currentLocation.doesLocationRespond();
    }

    public Location getMemberCurrentLocation() {
        double randomNumber = Math.random();
        double cumulativeProbability = 0.0;

        for (LocationWithProbability location : possibleLocations) {
            
            cumulativeProbability += location.regularity.value;

            if (randomNumber <= cumulativeProbability) {
                return location.location;
            }
        }

        //Fall back to first location
        return possibleLocations.get(0).location;
    }

}
