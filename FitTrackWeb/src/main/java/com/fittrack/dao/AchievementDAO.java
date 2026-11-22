package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.Achievement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC Data Access Object for UserAchievements.
 */
public class AchievementDAO {

    /**
     * Inserts a newly earned badge if absent. Returns true only if newly unlocked.
     */
    public boolean unlockIfNotEarned(int userId, String badgeCode, String title, String description) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO UserAchievements (user_id, badge_code, title, description, unlocked_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, badgeCode);
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setString(5, LocalDate.now().toString());
            return ps.executeUpdate() > 0;
        }
    }

    public ArrayList<Achievement> findByUser(int userId) throws SQLException {
        String sql = """
                SELECT * FROM UserAchievements
                WHERE user_id = ?
                ORDER BY achievement_id DESC
                """;
        ArrayList<Achievement> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Achievement(
                            rs.getInt("achievement_id"),
                            rs.getInt("user_id"),
                            rs.getString("badge_code"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("unlocked_at")
                    ));
                }
            }
        }
        return list;
    }

    public Map<String, Achievement> findByUserIndexed(int userId) throws SQLException {
        Map<String, Achievement> map = new LinkedHashMap<>();
        for (Achievement a : findByUser(userId)) {
            map.put(a.getBadgeCode(), a);
        }
        return map;
    }
}
