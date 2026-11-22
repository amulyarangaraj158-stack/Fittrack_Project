package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.WaterTracker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC Data Access Object for WaterIntake table.
 */
public class WaterDAO {

    public WaterTracker addWaterIntake(WaterTracker entry) throws SQLException {
        String sql = """
                INSERT INTO WaterIntake (user_id, amount_ml, intake_date, intake_time, note)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entry.getUserId());
            ps.setInt(2, entry.getAmountMl());
            ps.setString(3, entry.getIntakeDate());
            ps.setString(4, entry.getIntakeTime());
            ps.setString(5, entry.getNote());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entry.setWaterId(rs.getInt(1));
                }
            }
        }
        return entry;
    }

    public boolean deleteWaterIntake(int waterId, int userId) throws SQLException {
        String sql = "DELETE FROM WaterIntake WHERE water_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waterId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public int getTotalWaterByDate(int userId, String date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount_ml), 0) FROM WaterIntake WHERE user_id = ? AND intake_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Map<String, Integer> getDailyTotalsBetween(int userId, String startDate, String endDate) throws SQLException {
        String sql = """
                SELECT intake_date, COALESCE(SUM(amount_ml), 0) AS total_ml
                FROM WaterIntake
                WHERE user_id = ? AND intake_date BETWEEN ? AND ?
                GROUP BY intake_date
                ORDER BY intake_date ASC
                """;
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("intake_date"), rs.getInt("total_ml"));
                }
            }
        }
        return map;
    }

    public ArrayList<WaterTracker> findRecentByUser(int userId, int limit) throws SQLException {
        String sql = """
                SELECT * FROM WaterIntake
                WHERE user_id = ?
                ORDER BY intake_date DESC, water_id DESC
                LIMIT ?
                """;
        ArrayList<WaterTracker> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WaterTracker(
                            rs.getInt("water_id"),
                            rs.getInt("user_id"),
                            rs.getInt("amount_ml"),
                            rs.getString("intake_date"),
                            rs.getString("intake_time"),
                            rs.getString("note")
                    ));
                }
            }
        }
        return list;
    }
}
