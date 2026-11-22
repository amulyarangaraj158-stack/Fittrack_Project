package com.fittrack.service;

import com.fittrack.dao.GoalDAO;
import com.fittrack.model.FitnessGoal;

import java.sql.SQLException;

/**
 * Service layer for Fitness Goals retrieval, updates, and progress calculations.
 */
public class GoalService {

    private final GoalDAO goalDAO;

    public GoalService() {
        this.goalDAO = new GoalDAO();
    }

    public FitnessGoal getOrCreateGoal(int userId) throws SQLException {
        FitnessGoal goal = goalDAO.findByUserId(userId);
        if (goal == null) {
            goal = new FitnessGoal(userId, 8000, 2500, 5, 500.0);
            goal = goalDAO.saveOrUpdate(goal);
        }
        return goal;
    }

    public FitnessGoal updateGoal(int userId, int dailyStepTarget, int dailyWaterTargetMl,
                                  int weeklyWorkoutTarget, double dailyCalorieTarget) throws SQLException {
        FitnessGoal goal = new FitnessGoal(userId, dailyStepTarget, dailyWaterTargetMl,
                weeklyWorkoutTarget, dailyCalorieTarget);
        return goalDAO.saveOrUpdate(goal);
    }

    public int calculatePercentage(double current, double target) {
        if (target <= 0) {
            return 0;
        }
        int pct = (int) Math.round((current / target) * 100.0);
        return Math.max(0, Math.min(100, pct));
    }
}
