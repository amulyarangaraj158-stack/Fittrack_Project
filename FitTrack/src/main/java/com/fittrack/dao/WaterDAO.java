package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.WaterIntake;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Data Access Object for WaterIntake CRUD operations in SQLite.
 */
public class WaterDAO {

    public WaterIntake addWaterIntake(WaterIntake intake) throws SQLException {
        String sql = """
                INSERT INTO water_intake (user_id, amount_ml, intake_date, intake_time, note)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, intake.getUserId());
            ps.setInt(2, intake.getAmountMl());
            ps.setString(3, intake.getIntakeDate());
            ps.setString(4, intake.getIntakeTime());
            ps.setString(5, intake.getNote());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    intake.setWaterId(rs.getInt(1));
                }
            }
        }
        return intake;
    }

    public boolean deleteWaterIntake(int waterId, int userId) throws SQLException {
        String sql = "DELETE FROM water_intake WHERE water_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waterId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public int getTotalWaterByDate(int userId, String date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount_ml), 0) FROM water_intake WHERE user_id = ? AND intake_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public ArrayList<WaterIntake> findByUserAndDate(int userId, String date) throws SQLException {
        String sql = """
                SELECT * FROM water_intake
                WHERE user_id = ? AND intake_date = ?
                ORDER BY water_id DESC
                """;
        ArrayList<WaterIntake> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public ArrayList<WaterIntake> findRecentByUser(int userId, int limit) throws SQLException {
        String sql = """
                SELECT * FROM water_intake
                WHERE user_id = ?
                ORDER BY intake_date DESC, water_id DESC
                LIMIT ?
                """;
        ArrayList<WaterIntake> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private WaterIntake mapRow(ResultSet rs) throws SQLException {
        return new WaterIntake(
                rs.getInt("water_id"),
                rs.getInt("user_id"),
                rs.getInt("amount_ml"),
                rs.getString("intake_date"),
                rs.getString("intake_time"),
                rs.getString("note")
        );
    }
}
