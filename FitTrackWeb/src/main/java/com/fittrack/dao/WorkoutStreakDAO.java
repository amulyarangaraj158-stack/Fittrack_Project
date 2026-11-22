package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JDBC Data Access Object for WorkoutStreaks table.
 */
public class WorkoutStreakDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void upsertStreak(int userId, int currentStreak, int longestStreak, String lastActiveDate) throws SQLException {
        String now = LocalDateTime.now().format(TS_FORMAT);
        String sql = """
                INSERT INTO WorkoutStreaks (user_id, current_streak, longest_streak, last_active_date, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    current_streak = excluded.current_streak,
                    longest_streak = excluded.longest_streak,
                    last_active_date = excluded.last_active_date,
                    updated_at = excluded.updated_at
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, currentStreak);
            ps.setInt(3, longestStreak);
            ps.setString(4, lastActiveDate);
            ps.setString(5, now);
            ps.executeUpdate();
        }
    }
}
