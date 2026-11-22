package elevator.sim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * Main window (Unit 5).
 *  - BorderLayout for the overall frame
 *  - GridLayout for the floor panel and the status strip
 *  - Mouse events on every floor row (see FloorCell)
 */
public class ElevatorGUI extends JFrame implements SimulationListener {

    private static final long serialVersionUID = 1L;

    private final Simulation sim;
    private final FloorCell[] cells;
    private final Random random = new Random();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");

    private final JLabel lblFloor = statusLabel();
    private final JLabel lblDirection = statusLabel();
    private final JLabel lblDoor = statusLabel();
    private final JLabel lblLoad = statusLabel();
    private final JLabel lblPending = statusLabel();
    private final JLabel lblThread = statusLabel();
    private final JTextArea logArea = new JTextArea();
    private final DefaultTableModel tripModel = new DefaultTableModel(TripLogDAO.TRIP_COLUMNS, 0) {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JSpinner paxSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 12, 1));
    private final JToggleButton autoBtn = new JToggleButton("Auto traffic");

    private int selectedFrom = -1;

    public ElevatorGUI() {
        super("Elevator Controller Simulation - Group 16");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        sim = new Simulation(Simulation.DEFAULT_FLOORS, "jdbc:sqlite:elevator_trips.db", this);

        // ---- NORTH: door & status panel (GridLayout) ----
        JPanel statusPanel = new JPanel(new GridLayout(2, 6, 4, 0));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Door & Status Panel"));
        for (String h : new String[]{"Floor", "Direction", "Doors", "Load", "Pending", "Thread"}) {
            JLabel l = new JLabel(h, JLabel.CENTER);
            l.setFont(l.getFont().deriveFont(Font.PLAIN, 11f));
            l.setForeground(Color.GRAY);
            statusPanel.add(l);
        }
        statusPanel.add(lblFloor);
        statusPanel.add(lblDirection);
        statusPanel.add(lblDoor);
        statusPanel.add(lblLoad);
        statusPanel.add(lblPending);
        statusPanel.add(lblThread);
        add(statusPanel, BorderLayout.NORTH);

        // ---- CENTER: floor panel (GridLayout), top floor first ----
        Floor[] floors = sim.getFloors();
        JPanel floorPanel = new JPanel(new GridLayout(floors.length, 1));
        floorPanel.setBorder(BorderFactory.createTitledBorder("Floor Panel - click pickup floor, then destination floor"));
        cells = new FloorCell[floors.length];
        for (int i = floors.length - 1; i >= 0; i--) {
            cells[i] = new FloorCell(floors[i], sim.getCar(), this::onFloorClicked);
            floorPanel.add(cells[i]);
        }
        add(floorPanel, BorderLayout.CENTER);

        // ---- EAST: trip log table ----
        JTable table = new JTable(tripModel);
        table.setFillsViewportHeight(true);
        JScrollPane tripScroll = new JScrollPane(table);
        tripScroll.setBorder(BorderFactory.createTitledBorder("Trip Log (from database)"));
        tripScroll.setPreferredSize(new Dimension(380, 300));
        add(tripScroll, BorderLayout.EAST);

        // ---- SOUTH: controls + event log ----
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Passengers:"));
        controls.add(paxSpinner);
        JButton randomBtn = new JButton("Random request");
        randomBtn.addActionListener(e -> randomRequest());
        JButton overloadBtn = new JButton("Overload test");
        overloadBtn.addActionListener(e -> overloadTest());
        JButton clearBtn = new JButton("Clear selection");
        clearBtn.addActionListener(e -> clearSelection());
        autoBtn.addActionListener(e -> sim.setAutoTraffic(autoBtn.isSelected()));
        controls.add(randomBtn);
        controls.add(overloadBtn);
        controls.add(autoBtn);
        controls.add(clearBtn);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Event Log"));
        logScroll.setPreferredSize(new Dimension(800, 150));
        JPanel south = new JPanel(new BorderLayout());
        south.add(controls, BorderLayout.NORTH);
        south.add(logScroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // ---- refresh the status panel 10x per second ----
        new Timer(100, e -> refreshStatus()).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sim.stop();
                dispose();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
        refreshTripTable();
        sim.start();
        setVisible(true);
    }

    // ---------- mouse handling ----------

    private void onFloorClicked(int floorIndex, boolean rightClick) {
        if (rightClick) {
            clearSelection();
            return;
        }
        if (selectedFrom < 0) {
            selectedFrom = floorIndex;
            cells[floorIndex].setSelected(true);
            cells[floorIndex].repaint();
            return;
        }
        if (selectedFrom == floorIndex) {
            clearSelection();
            return;
        }
        int pax = (Integer) paxSpinner.getValue();
        int from = selectedFrom;
        clearSelection();
        submit(from, floorIndex, pax);
    }

    private void clearSelection() {
        if (selectedFrom >= 0) {
            cells[selectedFrom].setSelected(false);
            cells[selectedFrom].repaint();
        }
        selectedFrom = -1;
    }

    private void submit(int from, int to, int pax) {
        try {
            sim.getController().submitRequest(new Request(from, to, pax));
        } catch (OverloadException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Overload", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException e) {
            onLog("! " + e.getMessage());
        }
    }

    private void randomRequest() {
        int n = sim.getFloors().length;
        int from = random.nextInt(n);
        int to = random.nextInt(n - 1);
        if (to >= from) to++;
        submit(from, to, 1 + random.nextInt(4));
    }

    private void overloadTest() {
        int n = sim.getFloors().length;
        submit(0, n - 1, sim.getCar().getCapacity() + 2);   // more people than the car can carry
    }

    // ---------- periodic refresh ----------

    private void refreshStatus() {
        ElevatorCar car = sim.getCar();
        Floor f = sim.getFloors()[car.getCurrentFloor()];
        lblFloor.setText(f.getLabel());
        lblDirection.setText(car.getDirection().toString());
        lblDoor.setText(car.getDoorState().toString());
        lblLoad.setText(car.getLoad() + " / " + car.getCapacity());
        lblLoad.setForeground(car.getLoad() >= car.getCapacity() ? Color.RED : Color.BLACK);
        lblPending.setText(sim.getQueue().size() + " / " + sim.getQueue().getCapacity());
        Thread t = sim.getElevatorThread();
        lblThread.setText(t == null ? "NEW" : t.getState().toString());
        for (FloorCell c : cells) {
            c.repaint();
        }
    }

    private void refreshTripTable() {
        tripModel.setRowCount(0);
        List<String[]> rows = sim.getDao().recentTrips(50);
        for (String[] r : rows) {
            tripModel.addRow(r);
        }
    }

    // ---------- SimulationListener (called from other threads) ----------

    @Override
    public void onLog(String message) {
        String line = timeFmt.format(new Date()) + "  " + message + "\n";
        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onTripLogged() {
        SwingUtilities.invokeLater(this::refreshTripTable);
    }

    private static JLabel statusLabel() {
        JLabel l = new JLabel("-", JLabel.CENTER);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        return l;
    }
}
