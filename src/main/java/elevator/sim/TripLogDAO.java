package elevator.sim;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JDBC data-access class (Unit 4). Stores two tables: requests and trips.
 * Uses SQLite (single file, no server). If the driver is missing the
 * simulation still runs and trips are kept in memory instead.
 */
public class TripLogDAO implements AutoCloseable {

    public static final String[] TRIP_COLUMNS =
            {"Trip", "Req", "From", "To", "Pax", "Floors", "Start", "Ms"};

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");
    private final List<String[]> memoryTrips = new ArrayList<>();
    private Connection conn;
    private boolean enabled;
    private String status;
    private int memoryTripId = 1;

    public TripLogDAO(String jdbcUrl) {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(jdbcUrl);
            createTables();
            enabled = true;
            status = "JDBC connected: " + jdbcUrl;
        } catch (ClassNotFoundException e) {
            status = "SQLite driver not found - using in-memory trip log (see README)";
        } catch (SQLException e) {
            status = "Database error - using in-memory trip log: " + e.getMessage();
        }
    }

    public boolean isEnabled() { return enabled; }

    public String getStatus() { return status; }

    private void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS requests ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "from_floor INTEGER NOT NULL,"
                    + "to_floor INTEGER NOT NULL,"
                    + "passengers INTEGER NOT NULL,"
                    + "requested_at TEXT NOT NULL,"
                    + "status TEXT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS trips ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "request_id INTEGER NOT NULL,"
                    + "from_floor INTEGER NOT NULL,"
                    + "to_floor INTEGER NOT NULL,"
                    + "passengers INTEGER NOT NULL,"
                    + "floors_travelled INTEGER NOT NULL,"
                    + "started_at TEXT NOT NULL,"
                    + "duration_ms INTEGER NOT NULL,"
                    + "FOREIGN KEY (request_id) REFERENCES requests(id))");
        }
    }

    private String stamp(long millis) {
        return timeFmt.format(new Date(millis));
    }

    /** Inserts a PENDING request and returns its generated id (-1 if no DB). */
    public synchronized long insertRequest(Request r) {
        if (!enabled) return -1;
        String sql = "INSERT INTO requests(from_floor,to_floor,passengers,requested_at,status) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getFromFloor());
            ps.setInt(2, r.getToFloor());
            ps.setInt(3, r.getPassengers());
            ps.setString(4, stamp(r.getCreatedAt()));
            ps.setString(5, "PENDING");
            ps.executeUpdate();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("insertRequest failed: " + e.getMessage());
        }
        return -1;
    }

    public synchronized void updateRequestStatus(long requestId, String newStatus) {
        if (!enabled || requestId < 0) return;
        try (PreparedStatement ps = conn.prepareStatement("UPDATE requests SET status=? WHERE id=?")) {
            ps.setString(1, newStatus);
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateRequestStatus failed: " + e.getMessage());
        }
    }

    public synchronized void insertTrip(Request r, int floorsTravelled, long startMillis, long durationMs) {
        if (enabled) {
            String sql = "INSERT INTO trips(request_id,from_floor,to_floor,passengers,floors_travelled,started_at,duration_ms) "
                    + "VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, r.getDbId());
                ps.setInt(2, r.getFromFloor());
                ps.setInt(3, r.getToFloor());
                ps.setInt(4, r.getPassengers());
                ps.setInt(5, floorsTravelled);
                ps.setString(6, stamp(startMillis));
                ps.setLong(7, durationMs);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("insertTrip failed: " + e.getMessage());
            }
        } else {
            memoryTrips.add(0, new String[]{
                    String.valueOf(memoryTripId++), String.valueOf(r.getId()),
                    String.valueOf(r.getFromFloor()), String.valueOf(r.getToFloor()),
                    String.valueOf(r.getPassengers()), String.valueOf(floorsTravelled),
                    stamp(startMillis), String.valueOf(durationMs)});
        }
    }

    /** Most recent trips first, as rows matching TRIP_COLUMNS. */
    public synchronized List<String[]> recentTrips(int limit) {
        if (!enabled) {
            return new ArrayList<>(memoryTrips.subList(0, Math.min(limit, memoryTrips.size())));
        }
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT id,request_id,from_floor,to_floor,passengers,floors_travelled,started_at,duration_ms "
                + "FROM trips ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)});
                }
            }
        } catch (SQLException e) {
            System.err.println("recentTrips failed: " + e.getMessage());
        }
        return rows;
    }

    @Override
    public synchronized void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // nothing useful to do on shutdown
            }
        }
    }
}
