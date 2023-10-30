import java.util.List;
import java.util.concurrent.TimeUnit;

import enums.InternetSpeed;
import enums.Regularity;
import enums.ResponseLikelihood;

class Location {
    InternetSpeed internetSpeed;
    ResponseLikelihood responseLikelihood;

    public Location(InternetSpeed internetSpeed, ResponseLikelihood responseLikelihood) {
        this.internetSpeed = internetSpeed;
        this.responseLikelihood = responseLikelihood;
    }

    public boolean doesLocationRespond() {
        double probability = responseLikelihood.value;

        double randomNumber = Math.random();

        return randomNumber < probability;
    }
}

class LocationWithRegularity {
    Location location;
    Regularity regularity; //Represents the regularity with which the member is at this location

    public LocationWithRegularity(Location location, Regularity regularity) {
        this.location = location;
        this.regularity = regularity;
    }
}

public class MemberResponsiveness {
    List<LocationWithRegularity> possibleLocations;

    public MemberResponsiveness(List<LocationWithRegularity> locations) {
        this.possibleLocations = locations;
    }
    
    public boolean doesMemberRespond(Location location) {
        return location.doesLocationRespond();
    }

    public void delayResponse(Location location) throws InterruptedException {
        int delaySeconds = location.internetSpeed.delay;

        TimeUnit.SECONDS.sleep(delaySeconds);
    }

    public Location getMemberCurrentLocation() {
        double randomNumber = Math.random();
        double cumulativeProbability = 0.0;

        for (LocationWithRegularity locationWithProbability : possibleLocations) {
            
            cumulativeProbability += locationWithProbability.regularity.value;

            if (randomNumber <= cumulativeProbability) {
                return locationWithProbability.location;
            }
        }

        //Fall back to first location
        return possibleLocations.get(0).location;
    }

}
