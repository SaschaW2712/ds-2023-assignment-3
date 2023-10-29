package enums;

enum InternetSpeed {
    High(0),
    Medium(3),
    Low(5);

    public final int delay;

    private InternetSpeed(int delay) {
        this.delay = delay;
    }
}