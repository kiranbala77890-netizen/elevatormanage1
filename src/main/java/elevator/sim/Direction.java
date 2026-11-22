package elevator.sim;

/** Travel direction of the car. */
public enum Direction {
    UP("\u25B2 UP"), DOWN("\u25BC DOWN"), IDLE("\u25CF IDLE");

    private final String label;

    Direction(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
