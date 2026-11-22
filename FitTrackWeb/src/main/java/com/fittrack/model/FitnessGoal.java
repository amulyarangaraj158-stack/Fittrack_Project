package com.fittrack.model;

/**
 * Encapsulated model for a user's fitness goals (steps, water, weekly workouts, calories, active minutes, distance).
 */
public class FitnessGoal {

    private int goalId;
    private int userId;
    private int dailyStepTarget;
    private int dailyWaterTargetMl;
    private int weeklyWorkoutTarget;
    private double dailyCalorieTarget;
    private int activeMinutesTarget;
    private double distanceKmTarget;
    private String updatedAt;

    public FitnessGoal() {
        this.dailyStepTarget = 8000;
        this.dailyWaterTargetMl = 2500;
        this.weeklyWorkoutTarget = 5;
        this.dailyCalorieTarget = 500.0;
        this.activeMinutesTarget = 150;
        this.distanceKmTarget = 20.0;
    }

    public FitnessGoal(int userId, int dailyStepTarget, int dailyWaterTargetMl,
                       int weeklyWorkoutTarget, double dailyCalorieTarget) {
        this(userId, dailyStepTarget, dailyWaterTargetMl, weeklyWorkoutTarget, dailyCalorieTarget, 150, 20.0);
    }

    public FitnessGoal(int userId, int dailyStepTarget, int dailyWaterTargetMl,
                       int weeklyWorkoutTarget, double dailyCalorieTarget,
                       int activeMinutesTarget, double distanceKmTarget) {
        this.userId = userId;
        setDailyStepTarget(dailyStepTarget);
        setDailyWaterTargetMl(dailyWaterTargetMl);
        setWeeklyWorkoutTarget(weeklyWorkoutTarget);
        setDailyCalorieTarget(dailyCalorieTarget);
        setActiveMinutesTarget(activeMinutesTarget);
        setDistanceKmTarget(distanceKmTarget);
    }

    public FitnessGoal(int goalId, int userId, int dailyStepTarget, int dailyWaterTargetMl,
                       int weeklyWorkoutTarget, double dailyCalorieTarget,
                       int activeMinutesTarget, double distanceKmTarget, String updatedAt) {
        this.goalId = goalId;
        this.userId = userId;
        this.dailyStepTarget = dailyStepTarget;
        this.dailyWaterTargetMl = dailyWaterTargetMl;
        this.weeklyWorkoutTarget = weeklyWorkoutTarget;
        this.dailyCalorieTarget = dailyCalorieTarget;
        this.activeMinutesTarget = activeMinutesTarget > 0 ? activeMinutesTarget : 150;
        this.distanceKmTarget = distanceKmTarget > 0 ? distanceKmTarget : 20.0;
        this.updatedAt = updatedAt;
    }

    public int calculateCompletionPercentage(double current, double target) {
        if (target <= 0) {
            return 0;
        }
        return (int) Math.max(0, Math.min(100, Math.round((current / target) * 100.0)));
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
            throw new IllegalArgumentException("Daily step goal must be between 500 and 100,000.");
        }
        this.dailyStepTarget = dailyStepTarget;
    }

    public int getDailyWaterTargetMl() {
        return dailyWaterTargetMl;
    }

    public void setDailyWaterTargetMl(int dailyWaterTargetMl) {
        if (dailyWaterTargetMl < 500 || dailyWaterTargetMl > 10000) {
            throw new IllegalArgumentException("Daily water goal must be between 500 ml and 10,000 ml.");
        }
        this.dailyWaterTargetMl = dailyWaterTargetMl;
    }

    public int getWeeklyWorkoutTarget() {
        return weeklyWorkoutTarget;
    }

    public void setWeeklyWorkoutTarget(int weeklyWorkoutTarget) {
        if (weeklyWorkoutTarget < 1 || weeklyWorkoutTarget > 21) {
            throw new IllegalArgumentException("Weekly workout goal must be between 1 and 21.");
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

    public int getActiveMinutesTarget() {
        return activeMinutesTarget;
    }

    public void setActiveMinutesTarget(int activeMinutesTarget) {
        if (activeMinutesTarget < 15 || activeMinutesTarget > 2000) {
            throw new IllegalArgumentException("Weekly active minutes goal must be between 15 and 2,000 minutes.");
        }
        this.activeMinutesTarget = activeMinutesTarget;
    }

    public double getDistanceKmTarget() {
        return distanceKmTarget;
    }

    public void setDistanceKmTarget(double distanceKmTarget) {
        if (distanceKmTarget < 1 || distanceKmTarget > 1000) {
            throw new IllegalArgumentException("Weekly distance goal must be between 1 and 1,000 km.");
        }
        this.distanceKmTarget = Math.round(distanceKmTarget * 10.0) / 10.0;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
