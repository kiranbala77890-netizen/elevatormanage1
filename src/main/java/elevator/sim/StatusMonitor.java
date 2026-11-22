package elevator.sim;

/**
 * Lowest-priority daemon thread that reports the elevator thread's life-cycle
 * state (NEW, RUNNABLE, WAITING, TIMED_WAITING, TERMINATED) whenever it changes.
 */
public class StatusMonitor implements Runnable {

    private final Thread target;
    private final SimulationListener listener;

    public StatusMonitor(Thread target, SimulationListener listener) {
        this.target = target;
        this.listener = listener;
    }

    @Override
    public void run() {
        Thread.State last = null;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.State now = target.getState();
                if (now != last) {
                    listener.onLog("[monitor] " + target.getName() + " state: " + now);
                    last = now;
                }
                if (now == Thread.State.TERMINATED) return;
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
