package enums;

public enum Regularity {
    Always(1),
    Often(0.75),
    Sometimes(0.5),
    Rarely(0.25),
    Never(0);

    public final double value;

    private Regularity(double value) {
        this.value = value;
    }
}