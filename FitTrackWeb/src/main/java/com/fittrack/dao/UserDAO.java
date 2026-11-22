package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.PremiumUser;
import com.fittrack.model.RegularUser;
import com.fittrack.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JDBC Data Access Object for the Users table.
 */
public class UserDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public User createUserWithDefaultGoal(User user) throws SQLException {
        String insertUserSql = """
                INSERT INTO Users (full_name, email, password_hash, account_type, age, gender,
                                   height_cm, weight_kg, activity_preference, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertGoalSql = """
                INSERT INTO FitnessGoals (user_id, daily_step_target, daily_water_target_ml,
                                          weekly_workout_target, daily_calorie_target,
                                          active_minutes_target, distance_km_target, updated_at)
                VALUES (?, 8000, 2500, 5, 500.0, 150, 20.0, ?)
                """;
        String insertWeightSql = """
                INSERT INTO WeightHistory (user_id, weight_kg, recorded_date)
                VALUES (?, ?, ?)
                """;
        String insertSettingsSql = """
                INSERT INTO UserSettings (user_id, theme_mode, workout_reminders, water_reminders,
                                          goal_alerts, achievement_alerts, weekly_digest, updated_at)
                VALUES (?, 'light', 1, 1, 1, 1, 1, ?)
                """;

        String now = LocalDateTime.now().format(TS_FORMAT);
        String todayDate = java.time.LocalDate.now().toString();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int userId;
            try (PreparedStatement ps = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPasswordHash());
                ps.setString(4, user.getAccountType());
                ps.setInt(5, user.getAge());
                ps.setString(6, user.getGender());
                ps.setDouble(7, user.getHeightCm());
                ps.setDouble(8, user.getWeightKg());
                ps.setString(9, user.getActivityPreference());
                ps.setString(10, now);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        userId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated user_id.");
                    }
                }
            }

            try (PreparedStatement goalPs = conn.prepareStatement(insertGoalSql)) {
                goalPs.setInt(1, userId);
                goalPs.setString(2, now);
                goalPs.executeUpdate();
            }

            try (PreparedStatement weightPs = conn.prepareStatement(insertWeightSql)) {
                weightPs.setInt(1, userId);
                weightPs.setDouble(2, user.getWeightKg());
                weightPs.setString(3, todayDate);
                weightPs.executeUpdate();
            }

            try (PreparedStatement setPs = conn.prepareStatement(insertSettingsSql)) {
                setPs.setInt(1, userId);
                setPs.setString(2, now);
                setPs.executeUpdate();
            }

            conn.commit();
            user.setUserId(userId);
            user.setCreatedAt(now);
            return user;
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

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM Users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM Users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM Users WHERE user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean updateProfile(User user) throws SQLException {
        String sql = """
                UPDATE Users
                SET full_name = ?, account_type = ?, age = ?, gender = ?,
                    height_cm = ?, weight_kg = ?, activity_preference = ?
                WHERE user_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getAccountType());
            ps.setInt(3, user.getAge());
            ps.setString(4, user.getGender());
            ps.setDouble(5, user.getHeightCm());
            ps.setDouble(6, user.getWeightKg());
            ps.setString(7, user.getActivityPreference());
            ps.setInt(8, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePasswordHash(int userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE Users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM Users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String accountType = rs.getString("account_type");
        int age = rs.getInt("age");
        String gender = rs.getString("gender");
        double heightCm = rs.getDouble("height_cm");
        double weightKg = rs.getDouble("weight_kg");
        String activityPref = rs.getString("activity_preference");
        String createdAt = rs.getString("created_at");

        if (User.ACCOUNT_PREMIUM.equalsIgnoreCase(accountType)) {
            return new PremiumUser(id, fullName, email, passwordHash, age, gender,
                    heightCm, weightKg, activityPref, createdAt);
        }
        return new RegularUser(id, fullName, email, passwordHash, age, gender,
                heightCm, weightKg, activityPref, createdAt);
    }
}
