package enums;

//Creates a delay in seconds
public enum InternetSpeed {
    High(0),
    Medium(1),
    Low(2);

    public final int delay;

    private InternetSpeed(int delay) {
        this.delay = delay;
    }
}