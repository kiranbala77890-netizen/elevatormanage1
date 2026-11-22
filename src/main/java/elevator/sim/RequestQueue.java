package elevator.sim;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Thread-safe request queue (Unit 3: synchronized).
 * Producers (GUI clicks, traffic generator) call add();
 * the elevator thread blocks in take() until work arrives (wait/notify).
 */
public class RequestQueue {

    private final LinkedList<Request> pending = new LinkedList<>();
    private final int capacity;

    public RequestQueue(int capacity) {
        this.capacity = capacity;
    }

    /** Add a request or throw OverloadException if the queue is full. */
    public synchronized void add(Request r) throws OverloadException {
        if (pending.size() >= capacity) {
            throw new OverloadException("Request queue full (" + capacity + " pending) - " + r + " rejected");
        }
        pending.add(r);
        notifyAll();   // wake the elevator thread
    }

    /**
     * Blocks while empty, then removes the pending request whose pickup
     * floor is nearest to the car (shortest-seek-first).
     */
    public synchronized Request take(int currentFloor) throws InterruptedException {
        while (pending.isEmpty()) {
            wait();    // elevator thread enters WAITING state
        }
        Request best = pending.getFirst();
        for (Request r : pending) {
            if (Math.abs(r.getFromFloor() - currentFloor) < Math.abs(best.getFromFloor() - currentFloor)) {
                best = r;
            }
        }
        pending.remove(best);
        return best;
    }

    public synchronized int size() {
        return pending.size();
    }

    public synchronized List<Request> snapshot() {
        return new ArrayList<>(pending);
    }

    public int getCapacity() {
        return capacity;
    }
}
