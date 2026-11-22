package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.FitnessGoal;
import com.fittrack.model.GoalHistoryEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * JDBC Data Access Object for FitnessGoals and GoalHistory tables.
 */
public class GoalDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FitnessGoal findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM FitnessGoals WHERE user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FitnessGoal(
                            rs.getInt("goal_id"),
                            rs.getInt("user_id"),
                            rs.getInt("daily_step_target"),
                            rs.getInt("daily_water_target_ml"),
                            rs.getInt("weekly_workout_target"),
                            rs.getDouble("daily_calorie_target"),
                            rs.getInt("active_minutes_target"),
                            rs.getDouble("distance_km_target"),
                            rs.getString("updated_at")
                    );
                }
                return null;
            }
        }
    }

    public FitnessGoal saveOrUpdate(FitnessGoal goal) throws SQLException {
        String now = LocalDateTime.now().format(TS_FORMAT);
        FitnessGoal existing = findByUserId(goal.getUserId());

        if (existing == null) {
            String insertSql = """
                    INSERT INTO FitnessGoals (user_id, daily_step_target, daily_water_target_ml,
                                              weekly_workout_target, daily_calorie_target,
                                              active_minutes_target, distance_km_target, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, goal.getUserId());
                ps.setInt(2, goal.getDailyStepTarget());
                ps.setInt(3, goal.getDailyWaterTargetMl());
                ps.setInt(4, goal.getWeeklyWorkoutTarget());
                ps.setDouble(5, goal.getDailyCalorieTarget());
                ps.setInt(6, goal.getActiveMinutesTarget());
                ps.setDouble(7, goal.getDistanceKmTarget());
                ps.setString(8, now);
                ps.executeUpdate();
            }
        } else {
            String updateSql = """
                    UPDATE FitnessGoals
                    SET daily_step_target = ?, daily_water_target_ml = ?,
                        weekly_workout_target = ?, daily_calorie_target = ?,
                        active_minutes_target = ?, distance_km_target = ?, updated_at = ?
                    WHERE user_id = ?
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, goal.getDailyStepTarget());
                ps.setInt(2, goal.getDailyWaterTargetMl());
                ps.setInt(3, goal.getWeeklyWorkoutTarget());
                ps.setDouble(4, goal.getDailyCalorieTarget());
                ps.setInt(5, goal.getActiveMinutesTarget());
                ps.setDouble(6, goal.getDistanceKmTarget());
                ps.setString(7, now);
                ps.setInt(8, goal.getUserId());
                ps.executeUpdate();
            }
        }

        recordHistorySnapshot(goal, now);
        return findByUserId(goal.getUserId());
    }

    private void recordHistorySnapshot(FitnessGoal goal, String timestamp) throws SQLException {
        String sql = """
                INSERT INTO GoalHistory (user_id, daily_step_target, daily_water_target_ml,
                                         weekly_workout_target, daily_calorie_target,
                                         active_minutes_target, distance_km_target, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goal.getUserId());
            ps.setInt(2, goal.getDailyStepTarget());
            ps.setInt(3, goal.getDailyWaterTargetMl());
            ps.setInt(4, goal.getWeeklyWorkoutTarget());
            ps.setDouble(5, goal.getDailyCalorieTarget());
            ps.setInt(6, goal.getActiveMinutesTarget());
            ps.setDouble(7, goal.getDistanceKmTarget());
            ps.setString(8, timestamp);
            ps.executeUpdate();
        }
    }

    public ArrayList<GoalHistoryEntry> findGoalHistory(int userId, int limit) throws SQLException {
        String sql = """
                SELECT * FROM GoalHistory
                WHERE user_id = ?
                ORDER BY history_id DESC
                LIMIT ?
                """;
        ArrayList<GoalHistoryEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new GoalHistoryEntry(
                            rs.getInt("history_id"),
                            rs.getInt("user_id"),
                            rs.getInt("daily_step_target"),
                            rs.getInt("daily_water_target_ml"),
                            rs.getInt("weekly_workout_target"),
                            rs.getDouble("daily_calorie_target"),
                            rs.getInt("active_minutes_target"),
                            rs.getDouble("distance_km_target"),
                            rs.getString("recorded_at")
                    ));
                }
            }
        }
        return list;
    }
}
