package dataclasses;

import enums.Regularity;

public class LocationWithRegularity {
    Location location;
    Regularity regularity; //Represents the regularity with which the member is at this location

    public LocationWithRegularity(Location location, Regularity regularity) {
        this.location = location;
        this.regularity = regularity;
    }
}