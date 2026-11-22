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
 * Data Access Object for User CRUD operations in SQLite.
 */
public class UserDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Inserts a new user and their default fitness goals inside a single database transaction.
     */
    public User createUserWithDefaultGoal(User user) throws SQLException {
        String insertUserSql = """
                INSERT INTO users (full_name, email, password_hash, account_type, age, gender, height_cm, weight_kg, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertGoalSql = """
                INSERT INTO fitness_goals (user_id, daily_step_target, daily_water_target_ml, weekly_workout_target, daily_calorie_target, updated_at)
                VALUES (?, 8000, 2500, 5, 500.0, ?)
                """;

        String now = LocalDateTime.now().format(TS_FORMAT);
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int generatedUserId;
            try (PreparedStatement ps = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPasswordHash());
                ps.setString(4, user.getAccountType());
                ps.setInt(5, user.getAge());
                ps.setString(6, user.getGender());
                ps.setDouble(7, user.getHeightCm());
                ps.setDouble(8, user.getWeightKg());
                ps.setString(9, now);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedUserId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to obtain generated user_id.");
                    }
                }
            }

            try (PreparedStatement goalPs = conn.prepareStatement(insertGoalSql)) {
                goalPs.setInt(1, generatedUserId);
                goalPs.setString(2, now);
                goalPs.executeUpdate();
            }

            conn.commit();
            user.setUserId(generatedUserId);
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
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
                return null;
            }
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
                return null;
            }
        }
    }

    public boolean updateProfile(User user) throws SQLException {
        String sql = """
                UPDATE users
                SET full_name = ?, account_type = ?, age = ?, gender = ?, height_cm = ?, weight_kg = ?
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
            ps.setInt(7, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String accountType = rs.getString("account_type");
        int age = rs.getInt("age");
        String gender = rs.getString("gender");
        double heightCm = rs.getDouble("height_cm");
        double weightKg = rs.getDouble("weight_kg");
        String createdAt = rs.getString("created_at");

        if (User.ACCOUNT_PREMIUM.equalsIgnoreCase(accountType)) {
            return new PremiumUser(id, fullName, email, passwordHash, age, gender, heightCm, weightKg, createdAt);
        }
        return new RegularUser(id, fullName, email, passwordHash, age, gender, heightCm, weightKg, createdAt);
    }
}
