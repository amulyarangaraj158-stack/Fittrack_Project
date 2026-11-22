package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.WorkoutPlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * JDBC Data Access Object for WorkoutPlans.
 */
public class WorkoutPlanDAO {

    public WorkoutPlan addPlan(WorkoutPlan plan) throws SQLException {
        String sql = """
                INSERT INTO WorkoutPlans (user_id, day_of_week, workout_name, workout_type, planned_minutes, intensity, notes, completed_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, plan.getUserId());
            ps.setString(2, plan.getDayOfWeek());
            ps.setString(3, plan.getWorkoutName());
            ps.setString(4, plan.getWorkoutType());
            ps.setInt(5, plan.getPlannedMinutes());
            ps.setString(6, plan.getIntensity());
            ps.setString(7, plan.getNotes());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    plan.setPlanId(rs.getInt(1));
                }
            }
        }
        return plan;
    }

    public boolean updatePlan(WorkoutPlan plan) throws SQLException {
        String sql = """
                UPDATE WorkoutPlans
                SET day_of_week = ?, workout_name = ?, workout_type = ?,
                    planned_minutes = ?, intensity = ?, notes = ?
                WHERE plan_id = ? AND user_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plan.getDayOfWeek());
            ps.setString(2, plan.getWorkoutName());
            ps.setString(3, plan.getWorkoutType());
            ps.setInt(4, plan.getPlannedMinutes());
            ps.setString(5, plan.getIntensity());
            ps.setString(6, plan.getNotes());
            ps.setInt(7, plan.getPlanId());
            ps.setInt(8, plan.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public ArrayList<WorkoutPlan> findByUser(int userId) throws SQLException {
        String sql = """
                SELECT * FROM WorkoutPlans
                WHERE user_id = ?
                ORDER BY CASE day_of_week
                    WHEN 'Monday' THEN 1
                    WHEN 'Tuesday' THEN 2
                    WHEN 'Wednesday' THEN 3
                    WHEN 'Thursday' THEN 4
                    WHEN 'Friday' THEN 5
                    WHEN 'Saturday' THEN 6
                    WHEN 'Sunday' THEN 7
                    ELSE 8 END, plan_id ASC
                """;
        ArrayList<WorkoutPlan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public int countByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM WorkoutPlans WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public WorkoutPlan findById(int planId, int userId) throws SQLException {
        String sql = "SELECT * FROM WorkoutPlans WHERE plan_id = ? AND user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean markCompleted(int planId, int userId, String completedDate) throws SQLException {
        String sql = "UPDATE WorkoutPlans SET completed_date = ? WHERE plan_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, completedDate);
            ps.setInt(2, planId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletePlan(int planId, int userId) throws SQLException {
        String sql = "DELETE FROM WorkoutPlans WHERE plan_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private WorkoutPlan mapRow(ResultSet rs) throws SQLException {
        return new WorkoutPlan(
                rs.getInt("plan_id"),
                rs.getInt("user_id"),
                rs.getString("day_of_week"),
                rs.getString("workout_name"),
                rs.getString("workout_type"),
                rs.getInt("planned_minutes"),
                rs.getString("intensity"),
                rs.getString("notes"),
                rs.getString("completed_date")
        );
    }
}
