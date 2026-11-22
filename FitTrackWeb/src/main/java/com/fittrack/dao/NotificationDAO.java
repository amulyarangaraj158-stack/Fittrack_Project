package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.NotificationItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * JDBC Data Access Object for Notifications table.
 */
public class NotificationDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NotificationItem addNotification(int userId, String category, String title, String message) throws SQLException {
        String sql = """
                INSERT INTO Notifications (user_id, category, title, message, is_read, created_at)
                VALUES (?, ?, ?, ?, 0, ?)
                """;
        String now = LocalDateTime.now().format(TS_FORMAT);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setString(5, now);
            ps.executeUpdate();
            int id = 0;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
            return new NotificationItem(id, userId, category, title, message, false, now);
        }
    }

    public boolean existsWithTitleToday(int userId, String title, String todayPrefix) throws SQLException {
        String sql = """
                SELECT 1 FROM Notifications
                WHERE user_id = ? AND title = ? AND created_at LIKE ?
                LIMIT 1
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, todayPrefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public ArrayList<NotificationItem> findByUser(int userId, int limit) throws SQLException {
        String sql = """
                SELECT * FROM Notifications
                WHERE user_id = ?
                ORDER BY is_read ASC, notification_id DESC
                LIMIT ?
                """;
        ArrayList<NotificationItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new NotificationItem(
                            rs.getInt("notification_id"),
                            rs.getInt("user_id"),
                            rs.getString("category"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getInt("is_read") == 1,
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return list;
    }

    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean markAsRead(int notificationId, int userId) throws SQLException {
        String sql = "UPDATE Notifications SET is_read = 1 WHERE notification_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public void markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE Notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean deleteNotification(int notificationId, int userId) throws SQLException {
        String sql = "DELETE FROM Notifications WHERE notification_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
