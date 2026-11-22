package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.UserSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JDBC Data Access Object for UserSettings table.
 */
public class UserSettingsDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserSettings findOrCreate(int userId) throws SQLException {
        String selectSql = "SELECT * FROM UserSettings WHERE user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        UserSettings defaults = new UserSettings();
        defaults.setUserId(userId);
        return saveOrUpdate(defaults);
    }

    public UserSettings saveOrUpdate(UserSettings settings) throws SQLException {
        String now = LocalDateTime.now().format(TS_FORMAT);
        String sql = """
                INSERT INTO UserSettings (user_id, theme_mode, workout_reminders, water_reminders,
                                          goal_alerts, achievement_alerts, weekly_digest, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    theme_mode = excluded.theme_mode,
                    workout_reminders = excluded.workout_reminders,
                    water_reminders = excluded.water_reminders,
                    goal_alerts = excluded.goal_alerts,
                    achievement_alerts = excluded.achievement_alerts,
                    weekly_digest = excluded.weekly_digest,
                    updated_at = excluded.updated_at
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, settings.getUserId());
            ps.setString(2, settings.getThemeMode());
            ps.setInt(3, settings.isWorkoutReminders() ? 1 : 0);
            ps.setInt(4, settings.isWaterReminders() ? 1 : 0);
            ps.setInt(5, settings.isGoalAlerts() ? 1 : 0);
            ps.setInt(6, settings.isAchievementAlerts() ? 1 : 0);
            ps.setInt(7, settings.isWeeklyDigest() ? 1 : 0);
            ps.setString(8, now);
            ps.executeUpdate();
        }
        return findOrCreate(settings.getUserId());
    }

    private UserSettings mapRow(ResultSet rs) throws SQLException {
        return new UserSettings(
                rs.getInt("setting_id"),
                rs.getInt("user_id"),
                rs.getString("theme_mode"),
                rs.getInt("workout_reminders") == 1,
                rs.getInt("water_reminders") == 1,
                rs.getInt("goal_alerts") == 1,
                rs.getInt("achievement_alerts") == 1,
                rs.getInt("weekly_digest") == 1,
                rs.getString("updated_at")
        );
    }
}
