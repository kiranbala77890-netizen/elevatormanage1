package elevator.sim;

/** Callback so the simulation threads can report to the GUI (or the console). */
public interface SimulationListener {

    /** A line for the event log. May be called from any thread. */
    void onLog(String message);

    /** A trip was just written to the trip log. */
    void onTripLogged();
}
