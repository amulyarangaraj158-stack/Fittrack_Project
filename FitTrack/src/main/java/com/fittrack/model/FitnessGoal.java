package com.fittrack.model;

/**
 * Represents a user's personal daily and weekly fitness targets.
 * Demonstrates OOP Encapsulation.
 */
public class FitnessGoal {

    private int goalId;
    private int userId;
    private int dailyStepTarget;
    private int dailyWaterTargetMl;
    private int weeklyWorkoutTarget;
    private double dailyCalorieTarget;
    private String updatedAt;

    public FitnessGoal() {
        this.dailyStepTarget = 8000;
        this.dailyWaterTargetMl = 2500;
        this.weeklyWorkoutTarget = 5;
        this.dailyCalorieTarget = 500.0;
    }

    public FitnessGoal(int userId, int dailyStepTarget, int dailyWaterTargetMl,
                       int weeklyWorkoutTarget, double dailyCalorieTarget) {
        this.userId = userId;
        setDailyStepTarget(dailyStepTarget);
        setDailyWaterTargetMl(dailyWaterTargetMl);
        setWeeklyWorkoutTarget(weeklyWorkoutTarget);
        setDailyCalorieTarget(dailyCalorieTarget);
    }

    public FitnessGoal(int goalId, int userId, int dailyStepTarget, int dailyWaterTargetMl,
                       int weeklyWorkoutTarget, double dailyCalorieTarget, String updatedAt) {
        this.goalId = goalId;
        this.userId = userId;
        this.dailyStepTarget = dailyStepTarget;
        this.dailyWaterTargetMl = dailyWaterTargetMl;
        this.weeklyWorkoutTarget = weeklyWorkoutTarget;
        this.dailyCalorieTarget = dailyCalorieTarget;
        this.updatedAt = updatedAt;
    }

    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getDailyStepTarget() {
        return dailyStepTarget;
    }

    public void setDailyStepTarget(int dailyStepTarget) {
        if (dailyStepTarget < 500 || dailyStepTarget > 100000) {
            throw new IllegalArgumentException("Daily step target must be between 500 and 100,000.");
        }
        this.dailyStepTarget = dailyStepTarget;
    }

    public int getDailyWaterTargetMl() {
        return dailyWaterTargetMl;
    }

    public void setDailyWaterTargetMl(int dailyWaterTargetMl) {
        if (dailyWaterTargetMl < 500 || dailyWaterTargetMl > 10000) {
            throw new IllegalArgumentException("Daily water target must be between 500 ml and 10,000 ml.");
        }
        this.dailyWaterTargetMl = dailyWaterTargetMl;
    }

    public int getWeeklyWorkoutTarget() {
        return weeklyWorkoutTarget;
    }

    public void setWeeklyWorkoutTarget(int weeklyWorkoutTarget) {
        if (weeklyWorkoutTarget < 1 || weeklyWorkoutTarget > 21) {
            throw new IllegalArgumentException("Weekly workout target must be between 1 and 21 sessions.");
        }
        this.weeklyWorkoutTarget = weeklyWorkoutTarget;
    }

    public double getDailyCalorieTarget() {
        return dailyCalorieTarget;
    }

    public void setDailyCalorieTarget(double dailyCalorieTarget) {
        if (dailyCalorieTarget < 100 || dailyCalorieTarget > 5000) {
            throw new IllegalArgumentException("Daily calorie target must be between 100 and 5,000 kcal.");
        }
        this.dailyCalorieTarget = dailyCalorieTarget;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
