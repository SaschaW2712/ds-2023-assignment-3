package enums;

public enum ResponseLikelihood {
    Certain(1),
    Probable(0.9),
    Possible(0.6),
    Improbable(0.4),
    Impossible(0);

    public final double value;

    private ResponseLikelihood(double value) {
        this.value = value;
    }
}