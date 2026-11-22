package elevator.sim;

import java.util.Random;

/**
 * Background thread that simulates random passengers (Unit 3: thread with sleep).
 * Runs at normal priority - lower than the elevator thread.
 */
public class RequestGenerator implements Runnable {

    private final ElevatorController controller;
    private final int floorCount;
    private final SimulationListener listener;
    private final Random random = new Random();

    public RequestGenerator(ElevatorController controller, int floorCount, SimulationListener listener) {
        this.controller = controller;
        this.floorCount = floorCount;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1500 + random.nextInt(2500));
                int from = random.nextInt(floorCount);
                int to = random.nextInt(floorCount - 1);
                if (to >= from) to++;                       // guarantees to != from
                int pax = random.nextInt(100) < 10 ? 9 + random.nextInt(4)   // occasional overload
                                                   : 1 + random.nextInt(4);
                try {
                    controller.submitRequest(new Request(from, to, pax));
                } catch (OverloadException e) {
                    // already logged by the controller
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            listener.onLog("Traffic generator stopped");
        }
    }
}
