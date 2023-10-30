package dataclasses;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
