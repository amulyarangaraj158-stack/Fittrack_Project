package com.fittrack.dao;

import com.fittrack.database.DatabaseConnection;
import com.fittrack.model.FitnessGoal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Access Object for FitnessGoal CRUD operations in SQLite.
 */
public class GoalDAO {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FitnessGoal findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM fitness_goals WHERE user_id = ? LIMIT 1";
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
                    INSERT INTO fitness_goals (user_id, daily_step_target, daily_water_target_ml,
                                               weekly_workout_target, daily_calorie_target, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, goal.getUserId());
                ps.setInt(2, goal.getDailyStepTarget());
                ps.setInt(3, goal.getDailyWaterTargetMl());
                ps.setInt(4, goal.getWeeklyWorkoutTarget());
                ps.setDouble(5, goal.getDailyCalorieTarget());
                ps.setString(6, now);
                ps.executeUpdate();
            }
        } else {
            String updateSql = """
                    UPDATE fitness_goals
                    SET daily_step_target = ?, daily_water_target_ml = ?,
                        weekly_workout_target = ?, daily_calorie_target = ?, updated_at = ?
                    WHERE user_id = ?
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, goal.getDailyStepTarget());
                ps.setInt(2, goal.getDailyWaterTargetMl());
                ps.setInt(3, goal.getWeeklyWorkoutTarget());
                ps.setDouble(4, goal.getDailyCalorieTarget());
                ps.setString(5, now);
                ps.setInt(6, goal.getUserId());
                ps.executeUpdate();
            }
        }
        return findByUserId(goal.getUserId());
    }
}
