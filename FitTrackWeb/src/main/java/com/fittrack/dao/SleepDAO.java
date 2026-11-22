package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.SleepLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC Data Access Object for SleepLogs.
 */
public class SleepDAO {

    public SleepLog addSleepLog(SleepLog log) throws SQLException {
        String sql = """
                INSERT INTO SleepLogs (user_id, sleep_date, hours_slept, quality_rating, notes)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getUserId());
            ps.setString(2, log.getSleepDate());
            ps.setDouble(3, log.getHoursSlept());
            ps.setInt(4, log.getQualityRating());
            ps.setString(5, log.getNotes());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setSleepId(rs.getInt(1));
                }
            }
        }
        return log;
    }

    public ArrayList<SleepLog> findRecentByUser(int userId, int limit) throws SQLException {
        String sql = """
                SELECT * FROM SleepLogs
                WHERE user_id = ?
                ORDER BY sleep_date DESC, sleep_id DESC
                LIMIT ?
                """;
        ArrayList<SleepLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SleepLog(
                            rs.getInt("sleep_id"),
                            rs.getInt("user_id"),
                            rs.getString("sleep_date"),
                            rs.getDouble("hours_slept"),
                            rs.getInt("quality_rating"),
                            rs.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    public Map<String, Double> getDailyHoursBetween(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT sleep_date, COALESCE(AVG(hours_slept), 0) AS avg_hrs
                FROM SleepLogs
                WHERE user_id = ? AND sleep_date BETWEEN ? AND ?
                GROUP BY sleep_date
                ORDER BY sleep_date ASC
                """;
        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double val = Math.round(rs.getDouble("avg_hrs") * 10.0) / 10.0;
                    map.put(rs.getString("sleep_date"), val);
                }
            }
        }
        return map;
    }

    public double getAverageHoursLast7Days(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT COALESCE(AVG(hours_slept), 0)
                FROM SleepLogs
                WHERE user_id = ? AND sleep_date BETWEEN ? AND ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                double avg = rs.next() ? rs.getDouble(1) : 0.0;
                return Math.round(avg * 10.0) / 10.0;
            }
        }
    }

    public double getAverageQualityBetween(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT COALESCE(AVG(quality_rating), 0)
                FROM SleepLogs
                WHERE user_id = ? AND sleep_date BETWEEN ? AND ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                double avg = rs.next() ? rs.getDouble(1) : 0.0;
                return Math.round(avg * 10.0) / 10.0;
            }
        }
    }

    public int countLogsByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM SleepLogs WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean deleteSleepLog(int sleepId, int userId) throws SQLException {
        String sql = "DELETE FROM SleepLogs WHERE sleep_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sleepId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
