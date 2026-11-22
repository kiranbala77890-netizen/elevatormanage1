package elevator.sim;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Entry point.
 *   (no args)            start the Swing GUI
 *   --console [seconds]  headless run with random traffic (default 30 s)
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0 && args[0].equals("--console")) {
            int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 30;
            runConsole(seconds);
        } else {
            SwingUtilities.invokeLater(ElevatorGUI::new);
        }
    }

    private static void runConsole(int seconds) throws InterruptedException {
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
        SimulationListener printer = new SimulationListener() {
            @Override
            public void onLog(String message) {
                System.out.println(fmt.format(new Date()) + "  " + message);
            }

            @Override
            public void onTripLogged() {
                // trips are printed from the database at the end
            }
        };

        Simulation sim = new Simulation(Simulation.DEFAULT_FLOORS, "jdbc:sqlite:elevator_trips.db", printer);
        sim.start();
        sim.setAutoTraffic(true);
        Thread.sleep(seconds * 1000L);
        sim.setAutoTraffic(false);
        Thread.sleep(8000);   // let the car finish what is queued

        System.out.println("\n--- Last trips from the database ---");
        List<String[]> trips = sim.getDao().recentTrips(10);
        System.out.println(String.join(" | ", TripLogDAO.TRIP_COLUMNS));
        for (String[] row : trips) {
            System.out.println(String.join(" | ", row));
        }
        sim.stop();
    }
}
