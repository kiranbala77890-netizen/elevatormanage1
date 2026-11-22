package elevator.sim;

/**
 * Abstract base class for any elevator controller (Unit 2: abstract class).
 * Subclasses decide HOW requests are processed; this class holds the shared
 * state and the Runnable plumbing.
 */
public abstract class Controller implements Runnable {

    protected final ElevatorCar car;
    protected final Floor[] floors;
    protected final RequestQueue queue;
    protected volatile boolean running = true;

    protected Controller(ElevatorCar car, Floor[] floors, RequestQueue queue) {
        this.car = car;
        this.floors = floors;
        this.queue = queue;
    }

    /** Main service loop - implemented by concrete controllers. */
    public abstract void processRequests() throws InterruptedException;

    @Override
    public void run() {
        try {
            processRequests();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // shutting down
        }
    }

    public void shutdown() {
        running = false;
    }

    public ElevatorCar getCar() { return car; }
    public Floor[] getFloors() { return floors; }
    public RequestQueue getQueue() { return queue; }
}
