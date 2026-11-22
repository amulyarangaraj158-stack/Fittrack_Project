package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.WeightEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * JDBC Data Access Object for WeightHistory table.
 */
public class WeightHistoryDAO {

    public WeightEntry addEntry(WeightEntry entry) throws SQLException {
        String sql = """
                INSERT INTO WeightHistory (user_id, weight_kg, recorded_date)
                VALUES (?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entry.getUserId());
            ps.setDouble(2, entry.getWeightKg());
            ps.setString(3, entry.getRecordedDate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entry.setWeightId(rs.getInt(1));
                }
            }
        }
        return entry;
    }

    public boolean updateEntry(WeightEntry entry) throws SQLException {
        String sql = """
                UPDATE WeightHistory
                SET weight_kg = ?, recorded_date = ?
                WHERE weight_id = ? AND user_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, entry.getWeightKg());
            ps.setString(2, entry.getRecordedDate());
            ps.setInt(3, entry.getWeightId());
            ps.setInt(4, entry.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteEntry(int weightId, int userId) throws SQLException {
        String sql = "DELETE FROM WeightHistory WHERE weight_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, weightId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public ArrayList<WeightEntry> findByUserAscending(int userId, int limit) throws SQLException {
        String sql = """
                SELECT * FROM (
                    SELECT * FROM WeightHistory
                    WHERE user_id = ?
                    ORDER BY recorded_date DESC, weight_id DESC
                    LIMIT ?
                ) ORDER BY recorded_date ASC, weight_id ASC
                """;
        ArrayList<WeightEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WeightEntry(
                            rs.getInt("weight_id"),
                            rs.getInt("user_id"),
                            rs.getDouble("weight_kg"),
                            rs.getString("recorded_date")
                    ));
                }
            }
        }
        return list;
    }

    public ArrayList<WeightEntry> findByUserFiltered(int userId, String startDate, String endDate) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM WeightHistory WHERE user_id = ?");
        ArrayList<Object> params = new ArrayList<>();
        params.add(userId);
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND recorded_date >= ?");
            params.add(startDate.trim());
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND recorded_date <= ?");
            params.add(endDate.trim());
        }
        sql.append(" ORDER BY recorded_date ASC, weight_id ASC");

        ArrayList<WeightEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WeightEntry(
                            rs.getInt("weight_id"),
                            rs.getInt("user_id"),
                            rs.getDouble("weight_kg"),
                            rs.getString("recorded_date")
                    ));
                }
            }
        }
        return list;
    }
}
