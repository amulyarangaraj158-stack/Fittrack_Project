package com.fittrack.model;

/**
 * Historical snapshot of a user's fitness goal changes.
 */
public class GoalHistoryEntry {

    private int historyId;
    private int userId;
    private int dailyStepTarget;
    private int dailyWaterTargetMl;
    private int weeklyWorkoutTarget;
    private double dailyCalorieTarget;
    private int activeMinutesTarget;
    private double distanceKmTarget;
    private String recordedAt;

    public GoalHistoryEntry(int historyId, int userId, int dailyStepTarget, int dailyWaterTargetMl,
                            int weeklyWorkoutTarget, double dailyCalorieTarget,
                            int activeMinutesTarget, double distanceKmTarget, String recordedAt) {
        this.historyId = historyId;
        this.userId = userId;
        this.dailyStepTarget = dailyStepTarget;
        this.dailyWaterTargetMl = dailyWaterTargetMl;
        this.weeklyWorkoutTarget = weeklyWorkoutTarget;
        this.dailyCalorieTarget = dailyCalorieTarget;
        this.activeMinutesTarget = activeMinutesTarget;
        this.distanceKmTarget = distanceKmTarget;
        this.recordedAt = recordedAt;
    }

    public int getHistoryId() {
        return historyId;
    }

    public int getUserId() {
        return userId;
    }

    public int getDailyStepTarget() {
        return dailyStepTarget;
    }

    public int getDailyWaterTargetMl() {
        return dailyWaterTargetMl;
    }

    public int getWeeklyWorkoutTarget() {
        return weeklyWorkoutTarget;
    }

    public double getDailyCalorieTarget() {
        return dailyCalorieTarget;
    }

    public int getActiveMinutesTarget() {
        return activeMinutesTarget;
    }

    public double getDistanceKmTarget() {
        return distanceKmTarget;
    }

    public String getRecordedAt() {
        return recordedAt;
    }
}
