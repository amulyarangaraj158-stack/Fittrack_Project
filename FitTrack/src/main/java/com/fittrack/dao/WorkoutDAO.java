package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.Cycling;
import com.fittrack.model.Running;
import com.fittrack.model.StrengthTraining;
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

/**
 * Data Access Object for Workout CRUD operations.
 * Uses JDBC transactions to synchronize workouts and activity_records tables.
 */
public class WorkoutDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Adds a Workout and inserts its corresponding ActivityRecord atomically inside a transaction.
     */
    public Workout addWorkout(Workout workout) throws SQLException {
        String insertWorkoutSql = """
                INSERT INTO workouts (user_id, workout_type, duration_minutes, calories_burned,
                                      workout_date, intensity, steps, distance_km, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertActivitySql = """
                INSERT INTO activity_records (user_id, workout_id, activity_date, steps_count,
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
                ps.setString(2, workout.getWorkoutType());
                ps.setInt(3, workout.getDurationMinutes());
                ps.setDouble(4, workout.getCaloriesBurned());
                ps.setString(5, workout.getWorkoutDate());
                ps.setString(6, workout.getIntensity());
                ps.setInt(7, workout.getSteps());
                ps.setDouble(8, workout.getDistanceKm());
                ps.setString(9, workout.getNotes());
                ps.setString(10, now);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        workoutId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated workout_id.");
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
                actPs.setString(7, workout.getWorkoutType());
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

    /**
     * Updates a Workout and its linked ActivityRecord inside a transaction.
     */
    public boolean updateWorkout(Workout workout) throws SQLException {
        String updateWorkoutSql = """
                UPDATE workouts
                SET workout_type = ?, duration_minutes = ?, calories_burned = ?,
                    workout_date = ?, intensity = ?, steps = ?, distance_km = ?, notes = ?
                WHERE workout_id = ? AND user_id = ?
                """;
        String updateActivitySql = """
                UPDATE activity_records
                SET activity_date = ?, steps_count = ?, active_minutes = ?,
                    calories_burned = ?, activity_source = ?
                WHERE workout_id = ? AND user_id = ?
                """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int updatedRows;
            try (PreparedStatement ps = conn.prepareStatement(updateWorkoutSql)) {
                ps.setString(1, workout.getWorkoutType());
                ps.setInt(2, workout.getDurationMinutes());
                ps.setDouble(3, workout.getCaloriesBurned());
                ps.setString(4, workout.getWorkoutDate());
                ps.setString(5, workout.getIntensity());
                ps.setInt(6, workout.getSteps());
                ps.setDouble(7, workout.getDistanceKm());
                ps.setString(8, workout.getNotes());
                ps.setInt(9, workout.getWorkoutId());
                ps.setInt(10, workout.getUserId());
                updatedRows = ps.executeUpdate();
            }

            if (updatedRows > 0) {
                try (PreparedStatement actPs = conn.prepareStatement(updateActivitySql)) {
                    actPs.setString(1, workout.getWorkoutDate());
                    actPs.setInt(2, workout.getSteps());
                    actPs.setInt(3, workout.getDurationMinutes());
                    actPs.setDouble(4, workout.getCaloriesBurned());
                    actPs.setString(5, workout.getWorkoutType());
                    actPs.setInt(6, workout.getWorkoutId());
                    actPs.setInt(7, workout.getUserId());
                    actPs.executeUpdate();
                }
            }

            conn.commit();
            return updatedRows > 0;
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
        String sql = "DELETE FROM workouts WHERE workout_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public ArrayList<Workout> findAllByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM workouts WHERE user_id = ? ORDER BY workout_date DESC, workout_id DESC";
        ArrayList<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToWorkout(rs));
                }
            }
        }
        return list;
    }

    public ArrayList<Workout> findByUserAndType(int userId, String workoutType) throws SQLException {
        String sql = """
                SELECT * FROM workouts
                WHERE user_id = ? AND workout_type = ?
                ORDER BY workout_date DESC, workout_id DESC
                """;
        ArrayList<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, workoutType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToWorkout(rs));
                }
            }
        }
        return list;
    }

    public ArrayList<Workout> findByUserBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT * FROM workouts
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
                    list.add(mapRowToWorkout(rs));
                }
            }
        }
        return list;
    }

    public Workout findById(int workoutId, int userId) throws SQLException {
        String sql = "SELECT * FROM workouts WHERE workout_id = ? AND user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToWorkout(rs);
                }
                return null;
            }
        }
    }

    public double getTotalCaloriesByDate(int userId, String date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(calories_burned), 0) FROM workouts WHERE user_id = ? AND workout_date = ?";
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
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM workouts WHERE user_id = ? AND workout_date = ?";
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
        String sql = "SELECT COUNT(*) FROM workouts WHERE user_id = ? AND workout_date BETWEEN ? AND ?";
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

    /**
     * Instantiates the appropriate Workout subclass based on the stored workout_type.
     * Demonstrates Polymorphism: callers work with the abstract Workout reference.
     */
    private Workout mapRowToWorkout(ResultSet rs) throws SQLException {
        int workoutId = rs.getInt("workout_id");
        int userId = rs.getInt("user_id");
        String type = rs.getString("workout_type");
        int duration = rs.getInt("duration_minutes");
        double calories = rs.getDouble("calories_burned");
        String date = rs.getString("workout_date");
        String intensity = rs.getString("intensity");
        int steps = rs.getInt("steps");
        double distanceKm = rs.getDouble("distance_km");
        String notes = rs.getString("notes");
        String createdAt = rs.getString("created_at");

        return switch (type) {
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
            default ->
                    new Walking(workoutId, userId, duration, date, intensity, steps, distanceKm, notes, calories, createdAt);
        };
    }
}
