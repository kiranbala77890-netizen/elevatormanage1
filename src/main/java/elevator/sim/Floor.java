package elevator.sim;

/**
 * One floor of the building (Unit 1: class + constructors).
 * Floors are stored in an array inside {@link Simulation}.
 */
public class Floor {

    private final int number;
    private final String label;
    private int waiting;   // passengers currently waiting here

    /** Convenience constructor: floor 0 is labelled "G", others use their number. */
    public Floor(int number) {
        this(number, number == 0 ? "G" : String.valueOf(number));
    }

    public Floor(int number, String label) {
        if (number < 0) {
            throw new IllegalArgumentException("Floor number cannot be negative");
        }
        this.number = number;
        this.label = label;
    }

    public int getNumber() { return number; }

    public String getLabel() { return label; }

    public synchronized int getWaiting() { return waiting; }

    public synchronized void addWaiting(int n) { waiting += n; }

    public synchronized void removeWaiting(int n) { waiting = Math.max(0, waiting - n); }

    @Override
    public String toString() {
        return "Floor " + label;
    }
}
