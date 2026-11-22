package elevator.sim;

/**
 * The elevator car (Unit 1: class, constructor; Unit 2: implements Movable).
 * Holds position, direction, door state and passenger load.
 * Fields are volatile because the GUI thread reads them while the
 * elevator thread writes them.
 */
public class ElevatorCar implements Movable {

    public enum DoorState { CLOSED, OPENING, OPEN, CLOSING }

    private static final long DOOR_MS = 400;

    private final int totalFloors;
    private final int capacity;
    private final long floorTravelMs;

    private volatile int currentFloor = 0;
    private volatile Direction direction = Direction.IDLE;
    private volatile DoorState door = DoorState.CLOSED;
    private volatile int load = 0;

    public ElevatorCar(int totalFloors, int capacity, long floorTravelMs) {
        this.totalFloors = totalFloors;
        this.capacity = capacity;
        this.floorTravelMs = floorTravelMs;
    }

    // ---------- Movable ----------

    @Override
    public void moveUp() throws InterruptedException {
        checkCanMove();
        if (currentFloor >= totalFloors - 1) {
            throw new IllegalStateException("Already at top floor");
        }
        direction = Direction.UP;
        Thread.sleep(floorTravelMs);       // thread goes TIMED_WAITING
        currentFloor++;
    }

    @Override
    public void moveDown() throws InterruptedException {
        checkCanMove();
        if (currentFloor <= 0) {
            throw new IllegalStateException("Already at ground floor");
        }
        direction = Direction.DOWN;
        Thread.sleep(floorTravelMs);
        currentFloor--;
    }

    @Override
    public void stop() {
        direction = Direction.IDLE;
    }

    @Override
    public int getCurrentFloor() {
        return currentFloor;
    }

    // ---------- doors ----------

    public void openDoors() throws InterruptedException {
        door = DoorState.OPENING;
        Thread.sleep(DOOR_MS);
        door = DoorState.OPEN;
    }

    public void closeDoors() throws InterruptedException {
        door = DoorState.CLOSING;
        Thread.sleep(DOOR_MS);
        door = DoorState.CLOSED;
    }

    private void checkCanMove() {
        if (door != DoorState.CLOSED) {
            throw new IllegalStateException("Cannot move with doors " + door);
        }
    }

    // ---------- load ----------

    /** Passengers step in. Throws OverloadException if capacity would be exceeded. */
    public synchronized void board(int passengers) throws OverloadException {
        if (load + passengers > capacity) {
            throw new OverloadException("OVERLOAD: " + (load + passengers) + " passengers > capacity " + capacity);
        }
        load += passengers;
    }

    public synchronized void unload(int passengers) {
        load = Math.max(0, load - passengers);
    }

    // ---------- getters ----------

    public Direction getDirection() { return direction; }
    public DoorState getDoorState() { return door; }
    public int getLoad() { return load; }
    public int getCapacity() { return capacity; }
    public int getTotalFloors() { return totalFloors; }
}
