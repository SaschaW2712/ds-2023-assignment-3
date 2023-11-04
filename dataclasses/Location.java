package dataclasses;

import enums.InternetSpeed;
import enums.ResponseLikelihood;

public class Location {
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