package elevator.sim;

import java.util.concurrent.atomic.AtomicInteger;

/** A passenger request: pick up at fromFloor, drop at toFloor (Unit 1: class). */
public class Request {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private final int id;
    private final int fromFloor;
    private final int toFloor;
    private final int passengers;
    private final long createdAt;
    private long dbId = -1;   // primary key in the requests table

    public Request(int fromFloor, int toFloor, int passengers) {
        if (fromFloor == toFloor) {
            throw new IllegalArgumentException("Pickup and destination floors must differ");
        }
        if (passengers < 1) {
            throw new IllegalArgumentException("At least one passenger required");
        }
        this.id = SEQ.getAndIncrement();
        this.fromFloor = fromFloor;
        this.toFloor = toFloor;
        this.passengers = passengers;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public int getFromFloor() { return fromFloor; }
    public int getToFloor() { return toFloor; }
    public int getPassengers() { return passengers; }
    public long getCreatedAt() { return createdAt; }
    public long getDbId() { return dbId; }
    public void setDbId(long dbId) { this.dbId = dbId; }

    @Override
    public String toString() {
        return "Request#" + id + " [" + fromFloor + " -> " + toFloor + ", " + passengers + " pax]";
    }
}
