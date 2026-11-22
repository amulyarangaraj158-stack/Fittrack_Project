package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.ActivityRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Data Access Object for daily ActivityRecord entries (steps, active minutes, calories).
 */
public class ActivityRecordDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ActivityRecord addActivityRecord(ActivityRecord record) throws SQLException {
        String sql = """
                INSERT INTO activity_records (user_id, workout_id, activity_date, steps_count,
                                              active_minutes, calories_burned, activity_source, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String now = LocalDateTime.now().format(TS_FORMAT);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getUserId());
            if (record.getWorkoutId() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, record.getWorkoutId());
            }
            ps.setString(3, record.getActivityDate());
            ps.setInt(4, record.getStepsCount());
            ps.setInt(5, record.getActiveMinutes());
            ps.setDouble(6, record.getCaloriesBurned());
            ps.setString(7, record.getActivitySource());
            ps.setString(8, now);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setRecordId(rs.getInt(1));
                }
            }
        }
        record.setCreatedAt(now);
        return record;
    }

    public int getTotalStepsByDate(int userId, String date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(steps_count), 0) FROM activity_records WHERE user_id = ? AND activity_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int getTotalStepsBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(steps_count), 0) FROM activity_records
                WHERE user_id = ? AND activity_date BETWEEN ? AND ?
                """;
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

    public ArrayList<ActivityRecord> findByUserBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT * FROM activity_records
                WHERE user_id = ? AND activity_date BETWEEN ? AND ?
                ORDER BY activity_date DESC, record_id DESC
                """;
        ArrayList<ActivityRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int workoutIdRaw = rs.getInt("workout_id");
                    Integer workoutId = rs.wasNull() ? null : workoutIdRaw;
                    list.add(new ActivityRecord(
                            rs.getInt("record_id"),
                            rs.getInt("user_id"),
                            workoutId,
                            rs.getString("activity_date"),
                            rs.getInt("steps_count"),
                            rs.getInt("active_minutes"),
                            rs.getDouble("calories_burned"),
                            rs.getString("activity_source"),
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return list;
    }
}
