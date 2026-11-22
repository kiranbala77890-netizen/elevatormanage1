package elevator.sim;

/**
 * Custom checked exception (Unit 3).
 * Thrown when the car is over its weight/passenger limit,
 * or when too many requests are already waiting in the queue.
 */
public class OverloadException extends Exception {

    private static final long serialVersionUID = 1L;

    public OverloadException(String message) {
        super(message);
    }
}
