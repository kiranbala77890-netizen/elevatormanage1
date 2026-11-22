package elevator.sim;

/**
 * Concrete controller (Unit 2: extends abstract Controller).
 * Runs on the elevator thread: takes a request, drives to the pickup floor,
 * boards passengers (may throw OverloadException), drives to the destination,
 * and writes the trip to the database.
 */
public class ElevatorController extends Controller {

    private static final long DWELL_MS = 900;   // doors stay open this long

    private final TripLogDAO dao;
    private final SimulationListener listener;

    public ElevatorController(ElevatorCar car, Floor[] floors, RequestQueue queue,
                              TripLogDAO dao, SimulationListener listener) {
        super(car, floors, queue);
        this.dao = dao;
        this.listener = listener;
    }

    /**
     * Called by the GUI and the traffic generator (any thread).
     * Throws OverloadException if the queue is full.
     */
    public void submitRequest(Request r) throws OverloadException {
        if (r.getFromFloor() < 0 || r.getFromFloor() >= floors.length
                || r.getToFloor() < 0 || r.getToFloor() >= floors.length) {
            throw new IllegalArgumentException("Floor out of range: " + r);
        }
        r.setDbId(dao.insertRequest(r));
        floors[r.getFromFloor()].addWaiting(r.getPassengers());
        listener.onLog("New " + r + " (pending after: " + (queue.size() + 1) + ")");
        try {
            queue.add(r);
        } catch (OverloadException e) {
            floors[r.getFromFloor()].removeWaiting(r.getPassengers());
            dao.updateRequestStatus(r.getDbId(), "REJECTED_QUEUE_FULL");
            listener.onLog("! " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void processRequests() throws InterruptedException {
        listener.onLog("Controller started - waiting for requests");
        while (running) {
            Request r = queue.take(car.getCurrentFloor());   // blocks (WAITING)
            serve(r);
        }
    }

    private void serve(Request r) throws InterruptedException {
        long start = System.currentTimeMillis();
        int travelled = 0;
        dao.updateRequestStatus(r.getDbId(), "IN_PROGRESS");
        listener.onLog("Serving " + r);

        travelled += goTo(r.getFromFloor());
        car.openDoors();
        floors[r.getFromFloor()].removeWaiting(r.getPassengers());

        try {
            car.board(r.getPassengers());
        } catch (OverloadException e) {
            listener.onLog("! " + e.getMessage() + " at " + floors[r.getFromFloor()] + " - " + r + " cancelled");
            dao.updateRequestStatus(r.getDbId(), "REJECTED_OVERLOAD");
            Thread.sleep(DWELL_MS);          // buzzer, doors stay open
            car.closeDoors();
            car.stop();
            return;
        }

        listener.onLog(r.getPassengers() + " boarded at " + floors[r.getFromFloor()]
                + "  (load " + car.getLoad() + "/" + car.getCapacity() + ")");
        Thread.sleep(DWELL_MS);
        car.closeDoors();

        travelled += goTo(r.getToFloor());
        car.openDoors();
        car.unload(r.getPassengers());
        listener.onLog("Arrived " + floors[r.getToFloor()] + " - " + r.getPassengers() + " exited");
        Thread.sleep(DWELL_MS);
        car.closeDoors();
        car.stop();

        long duration = System.currentTimeMillis() - start;
        dao.updateRequestStatus(r.getDbId(), "COMPLETED");
        dao.insertTrip(r, travelled, start, duration);
        listener.onLog("Trip logged: " + r + ", " + travelled + " floors, " + duration + " ms");
        listener.onTripLogged();
    }

    /** Drives the car to the target floor; returns floors moved. */
    private int goTo(int target) throws InterruptedException {
        int moved = 0;
        while (car.getCurrentFloor() != target) {
            if (car.getCurrentFloor() < target) {
                car.moveUp();
            } else {
                car.moveDown();
            }
            moved++;
        }
        car.stop();
        return moved;
    }
}
