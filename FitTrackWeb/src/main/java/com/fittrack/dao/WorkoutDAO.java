package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.CustomWorkout;
import com.fittrack.model.Cycling;
import com.fittrack.model.HIIT;
import com.fittrack.model.Running;
import com.fittrack.model.StrengthTraining;
import com.fittrack.model.Swimming;
import com.fittrack.model.Walking;
import com.fittrack.model.Workout;
import com.fittrack.model.Yoga;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC Data Access Object for Workouts and synchronized ActivityRecords.
 */
public class WorkoutDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Workout addWorkout(Workout workout) throws SQLException {
        String insertWorkoutSql = """
                INSERT INTO Workouts (user_id, workout_name, workout_type, duration_minutes, calories_burned,
                                      workout_date, intensity, steps, distance_km, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertActivitySql = """
                INSERT INTO ActivityRecords (user_id, workout_id, activity_date, steps_count,
                                             active_minutes, calories_burned, activity_source, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String now = LocalDateTime.now().format(TS_FORMAT);
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int workoutId;
            try (PreparedStatement ps = conn.prepareStatement(insertWorkoutSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, workout.getUserId());
                ps.setString(2, workout.getWorkoutName());
                ps.setString(3, workout.getWorkoutType());
                ps.setInt(4, workout.getDurationMinutes());
                ps.setDouble(5, workout.getCaloriesBurned());
                ps.setString(6, workout.getWorkoutDate());
                ps.setString(7, workout.getIntensity());
                ps.setInt(8, workout.getSteps());
                ps.setDouble(9, workout.getDistanceKm());
                ps.setString(10, workout.getNotes());
                ps.setString(11, now);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        workoutId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve workout_id.");
                    }
                }
            }

            try (PreparedStatement actPs = conn.prepareStatement(insertActivitySql)) {
                actPs.setInt(1, workout.getUserId());
                actPs.setInt(2, workoutId);
                actPs.setString(3, workout.getWorkoutDate());
                actPs.setInt(4, workout.getSteps());
                actPs.setInt(5, workout.getDurationMinutes());
                actPs.setDouble(6, workout.getCaloriesBurned());
                actPs.setString(7, workout.getWorkoutName());
                actPs.setString(8, now);
                actPs.executeUpdate();
            }

            conn.commit();
            workout.setWorkoutId(workoutId);
            workout.setCreatedAt(now);
            return workout;
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean updateWorkout(Workout workout) throws SQLException {
        String updateWorkoutSql = """
                UPDATE Workouts
                SET workout_name = ?, workout_type = ?, duration_minutes = ?, calories_burned = ?,
                    workout_date = ?, intensity = ?, steps = ?, distance_km = ?, notes = ?
                WHERE workout_id = ? AND user_id = ?
                """;
        String updateActivitySql = """
                UPDATE ActivityRecords
                SET activity_date = ?, steps_count = ?, active_minutes = ?,
                    calories_burned = ?, activity_source = ?
                WHERE workout_id = ? AND user_id = ?
                """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int rows;
            try (PreparedStatement ps = conn.prepareStatement(updateWorkoutSql)) {
                ps.setString(1, workout.getWorkoutName());
                ps.setString(2, workout.getWorkoutType());
                ps.setInt(3, workout.getDurationMinutes());
                ps.setDouble(4, workout.getCaloriesBurned());
                ps.setString(5, workout.getWorkoutDate());
                ps.setString(6, workout.getIntensity());
                ps.setInt(7, workout.getSteps());
                ps.setDouble(8, workout.getDistanceKm());
                ps.setString(9, workout.getNotes());
                ps.setInt(10, workout.getWorkoutId());
                ps.setInt(11, workout.getUserId());
                rows = ps.executeUpdate();
            }

            if (rows > 0) {
                try (PreparedStatement actPs = conn.prepareStatement(updateActivitySql)) {
                    actPs.setString(1, workout.getWorkoutDate());
                    actPs.setInt(2, workout.getSteps());
                    actPs.setInt(3, workout.getDurationMinutes());
                    actPs.setDouble(4, workout.getCaloriesBurned());
                    actPs.setString(5, workout.getWorkoutName());
                    actPs.setInt(6, workout.getWorkoutId());
                    actPs.setInt(7, workout.getUserId());
                    actPs.executeUpdate();
                }
            }

            conn.commit();
            return rows > 0;
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean deleteWorkout(int workoutId, int userId) throws SQLException {
        String sql = "DELETE FROM Workouts WHERE workout_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public Workout findById(int workoutId, int userId) throws SQLException {
        String sql = "SELECT * FROM Workouts WHERE workout_id = ? AND user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public ArrayList<Workout> findAllByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM Workouts WHERE user_id = ? ORDER BY workout_date DESC, workout_id DESC";
        ArrayList<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public ArrayList<Workout> findByUserFiltered(int userId, String startDate, String endDate, String query) throws SQLException {
        return findByUserFilteredAndSorted(userId, startDate, endDate, "", "", query, "date", "desc");
    }

    public ArrayList<Workout> findByUserFilteredAndSorted(int userId, String startDate, String endDate,
                                                          String typeFilter, String intensityFilter,
                                                          String query, String sortBy, String sortDir) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM Workouts WHERE user_id = ?");
        ArrayList<Object> params = new ArrayList<>();
        params.add(userId);

        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND workout_date >= ?");
            params.add(startDate.trim());
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND workout_date <= ?");
            params.add(endDate.trim());
        }
        if (typeFilter != null && !typeFilter.isBlank() && !"ALL".equalsIgnoreCase(typeFilter.trim())) {
            sql.append(" AND workout_type = ?");
            params.add(typeFilter.trim());
        }
        if (intensityFilter != null && !intensityFilter.isBlank() && !"ALL".equalsIgnoreCase(intensityFilter.trim())) {
            sql.append(" AND LOWER(intensity) = LOWER(?)");
            params.add(intensityFilter.trim());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(workout_name, workout_type)) LIKE ? OR LOWER(workout_type) LIKE ? OR LOWER(COALESCE(notes, '')) LIKE ? OR LOWER(intensity) LIKE ?)");
            String pattern = "%" + query.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        // Strict SQL column & direction whitelist (SQL injection safe)
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        String orderClause = switch (sortBy == null ? "date" : sortBy.toLowerCase()) {
            case "type" -> "workout_type " + dir + ", workout_date DESC, workout_id DESC";
            case "duration" -> "duration_minutes " + dir + ", workout_date DESC, workout_id DESC";
            case "calories" -> "calories_burned " + dir + ", workout_date DESC, workout_id DESC";
            case "name" -> "COALESCE(workout_name, workout_type) " + dir + ", workout_date DESC";
            default -> "workout_date " + dir + ", workout_id " + dir;
        };
        sql.append(" ORDER BY ").append(orderClause);

        ArrayList<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public ArrayList<Workout> findByUserBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT * FROM Workouts
                WHERE user_id = ? AND workout_date BETWEEN ? AND ?
                ORDER BY workout_date ASC, workout_id ASC
                """;
        ArrayList<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Map<String, Object> getSummaryBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS sessions,
                       COALESCE(SUM(duration_minutes), 0) AS total_minutes,
                       COALESCE(SUM(calories_burned), 0) AS total_calories,
                       COALESCE(SUM(distance_km), 0) AS total_distance,
                       COALESCE(SUM(steps), 0) AS workout_steps
                FROM Workouts
                WHERE user_id = ? AND workout_date BETWEEN ? AND ?
                """;
        Map<String, Object> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("sessions", rs.getInt("sessions"));
                    map.put("minutes", rs.getInt("total_minutes"));
                    map.put("calories", Math.round(rs.getDouble("total_calories") * 10.0) / 10.0);
                    map.put("distanceKm", Math.round(rs.getDouble("total_distance") * 10.0) / 10.0);
                    map.put("workoutSteps", rs.getInt("workout_steps"));
                }
            }
        }
        return map;
    }

    public double getTotalCaloriesByDate(int userId, String date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(calories_burned), 0) FROM Workouts WHERE user_id = ? AND workout_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    public int getTotalDurationByDate(int userId, String date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM Workouts WHERE user_id = ? AND workout_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countWorkoutsBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Workouts WHERE user_id = ? AND workout_date BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countDistinctTypesByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT workout_type) FROM Workouts WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public ArrayList<String> findActiveDatesByUser(int userId) throws SQLException {
        String sql = """
                SELECT DISTINCT active_date FROM (
                    SELECT workout_date AS active_date FROM Workouts WHERE user_id = ?
                    UNION
                    SELECT activity_date AS active_date FROM ActivityRecords WHERE user_id = ? AND steps_count >= 3000
                )
                ORDER BY active_date DESC
                """;
        ArrayList<String> dates = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dates.add(rs.getString("active_date"));
                }
            }
        }
        return dates;
    }

    private Workout mapRow(ResultSet rs) throws SQLException {
        int workoutId = rs.getInt("workout_id");
        int userId = rs.getInt("user_id");
        String workoutName = rs.getString("workout_name");
        String type = rs.getString("workout_type");
        int duration = rs.getInt("duration_minutes");
        double calories = rs.getDouble("calories_burned");
        String date = rs.getString("workout_date");
        String intensity = rs.getString("intensity");
        int steps = rs.getInt("steps");
        double distanceKm = rs.getDouble("distance_km");
        String notes = rs.getString("notes");
        String createdAt = rs.getString("created_at");

        Workout workout = switch (type) {
            case Walking.TYPE_NAME ->
                    new Walking(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            case Running.TYPE_NAME ->
                    new Running(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            case Cycling.TYPE_NAME ->
                    new Cycling(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            case Yoga.TYPE_NAME ->
                    new Yoga(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            case StrengthTraining.TYPE_NAME ->
                    new StrengthTraining(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            case Swimming.TYPE_NAME ->
                    new Swimming(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            case HIIT.TYPE_NAME ->
                    new HIIT(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            case CustomWorkout.TYPE_NAME ->
                    new CustomWorkout(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
            default ->
                    new CustomWorkout(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
        };
        workout.setWorkoutName(workoutName);
        return workout;
    }
}
