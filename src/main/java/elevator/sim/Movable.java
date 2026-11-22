package elevator.sim;

/**
 * Anything that can travel between floors (Unit 2: interface).
 * ElevatorCar implements this contract.
 */
public interface Movable {

    /** Move one floor up. Takes time, so the caller's thread sleeps. */
    void moveUp() throws InterruptedException;

    /** Move one floor down. */
    void moveDown() throws InterruptedException;

    /** Stop and become idle. */
    void stop();

    /** Floor index the car is currently at (0 = ground). */
    int getCurrentFloor();
}
