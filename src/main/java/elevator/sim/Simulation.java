package elevator.sim;

/**
 * Wires everything together: floors array, car, queue, DAO, controller and the
 * three threads with different priorities (Unit 3: priorities).
 */
public class Simulation {

    public static final int DEFAULT_FLOORS = 8;
    public static final int CAPACITY = 8;          // passengers
    public static final int MAX_PENDING = 6;       // queue limit
    public static final long FLOOR_TRAVEL_MS = 600;

    private final Floor[] floors;
    private final ElevatorCar car;
    private final RequestQueue queue;
    private final TripLogDAO dao;
    private final ElevatorController controller;
    private final SimulationListener listener;

    private Thread elevatorThread;
    private Thread monitorThread;
    private Thread generatorThread;

    public Simulation(int floorCount, String jdbcUrl, SimulationListener listener) {
        this.listener = listener;
        this.floors = new Floor[floorCount];
        for (int i = 0; i < floorCount; i++) {
            floors[i] = new Floor(i);
        }
        this.car = new ElevatorCar(floorCount, CAPACITY, FLOOR_TRAVEL_MS);
        this.queue = new RequestQueue(MAX_PENDING);
        this.dao = new TripLogDAO(jdbcUrl);
        this.controller = new ElevatorController(car, floors, queue, dao, listener);
        listener.onLog(dao.getStatus());
    }

    public void start() {
        elevatorThread = new Thread(controller, "Elevator-Thread");
        elevatorThread.setPriority(Thread.MAX_PRIORITY - 2);   // 8: most important
        listener.onLog("[monitor] Elevator-Thread state: " + elevatorThread.getState());   // NEW

        monitorThread = new Thread(new StatusMonitor(elevatorThread, listener), "Monitor-Thread");
        monitorThread.setPriority(Thread.MIN_PRIORITY);        // 1: least important
        monitorThread.setDaemon(true);

        elevatorThread.start();
        monitorThread.start();
    }

    /** Turns random passenger traffic on or off. */
    public synchronized void setAutoTraffic(boolean on) {
        if (on && generatorThread == null) {
            generatorThread = new Thread(new RequestGenerator(controller, floors.length, listener), "Traffic-Thread");
            generatorThread.setPriority(Thread.NORM_PRIORITY);  // 5
            generatorThread.setDaemon(true);
            generatorThread.start();
            listener.onLog("Auto traffic ON");
        } else if (!on && generatorThread != null) {
            generatorThread.interrupt();
            generatorThread = null;
        }
    }

    public synchronized boolean isAutoTraffic() {
        return generatorThread != null;
    }

    public void stop() {
        setAutoTraffic(false);
        controller.shutdown();
        if (elevatorThread != null) elevatorThread.interrupt();
        if (monitorThread != null) monitorThread.interrupt();
        try {
            if (elevatorThread != null) elevatorThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        dao.close();
    }

    public Floor[] getFloors() { return floors; }
    public ElevatorCar getCar() { return car; }
    public RequestQueue getQueue() { return queue; }
    public TripLogDAO getDao() { return dao; }
    public ElevatorController getController() { return controller; }
    public Thread getElevatorThread() { return elevatorThread; }
}
