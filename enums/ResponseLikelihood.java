package enums;

public enum ResponseLikelihood {
    Certain(1),
    Probable(0.75),
    Possible(0.5),
    Improbable(0.25),
    Impossible(0);

    public final double value;

    private ResponseLikelihood(double value) {
        this.value = value;
    }
}