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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC Data Access Object for ActivityRecords table.
 */
public class ActivityRecordDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ActivityRecord addActivityRecord(ActivityRecord record) throws SQLException {
        String sql = """
                INSERT INTO ActivityRecords (user_id, workout_id, activity_date, steps_count,
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

    public ArrayList<ActivityRecord> findRecentByUser(int userId, int limit) throws SQLException {
        String sql = """
                SELECT * FROM ActivityRecords
                WHERE user_id = ?
                ORDER BY activity_date DESC, record_id DESC
                LIMIT ?
                """;
        ArrayList<ActivityRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int wid = rs.getInt("workout_id");
                    Integer workoutId = rs.wasNull() ? null : wid;
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

    public boolean deleteManualRecord(int recordId, int userId) throws SQLException {
        String sql = "DELETE FROM ActivityRecords WHERE record_id = ? AND user_id = ? AND workout_id IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public int getTotalStepsByDate(int userId, String date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(steps_count), 0) FROM ActivityRecords WHERE user_id = ? AND activity_date = ?";
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
                SELECT COALESCE(SUM(steps_count), 0) FROM ActivityRecords
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

    public Map<String, Integer> getDailyStepsBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT activity_date, COALESCE(SUM(steps_count), 0) AS total_steps
                FROM ActivityRecords
                WHERE user_id = ? AND activity_date BETWEEN ? AND ?
                GROUP BY activity_date
                ORDER BY activity_date ASC
                """;
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("activity_date"), rs.getInt("total_steps"));
                }
            }
        }
        return map;
    }
}
