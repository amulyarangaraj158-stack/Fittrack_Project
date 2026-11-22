package com.fittrack.model;

import com.fittrack.interfaces.Trackable;

/**
 * Represents a daily physical activity record stored in the activity_records table.
 * Links step counts, active minutes, and calories either to a Workout or a manual step entry.
 */
public class ActivityRecord implements Trackable {

    private int recordId;
    private int userId;
    private Integer workoutId;
    private String activityDate;
    private int stepsCount;
    private int activeMinutes;
    private double caloriesBurned;
    private String activitySource;
    private String createdAt;

    public ActivityRecord() {
    }

    public ActivityRecord(int userId, Integer workoutId, String activityDate,
                          int stepsCount, int activeMinutes, double caloriesBurned, String activitySource) {
        this.userId = userId;
        this.workoutId = workoutId;
        this.activityDate = activityDate;
        setStepsCount(stepsCount);
        setActiveMinutes(activeMinutes);
        setCaloriesBurned(caloriesBurned);
        this.activitySource = activitySource;
    }

    public ActivityRecord(int recordId, int userId, Integer workoutId, String activityDate,
                          int stepsCount, int activeMinutes, double caloriesBurned,
                          String activitySource, String createdAt) {
        this.recordId = recordId;
        this.userId = userId;
        this.workoutId = workoutId;
        this.activityDate = activityDate;
        this.stepsCount = stepsCount;
        this.activeMinutes = activeMinutes;
        this.caloriesBurned = caloriesBurned;
        this.activitySource = activitySource;
        this.createdAt = createdAt;
    }

    @Override
    public String trackActivity() {
        return String.format("%s: %d steps, %d min, %.1f kcal on %s",
                activitySource, stepsCount, activeMinutes, caloriesBurned, activityDate);
    }

    @Override
    public int getDurationMinutes() {
        return activeMinutes;
    }

    @Override
    public double getCaloriesBurned() {
        return caloriesBurned;
    }

    @Override
    public String getActivityDate() {
        return activityDate;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(Integer workoutId) {
        this.workoutId = workoutId;
    }

    public void setActivityDate(String activityDate) {
        this.activityDate = activityDate;
    }

    public int getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(int stepsCount) {
        if (stepsCount < 0) {
            throw new IllegalArgumentException("Steps count cannot be negative.");
        }
        this.stepsCount = stepsCount;
    }

    public int getActiveMinutes() {
        return activeMinutes;
    }

    public void setActiveMinutes(int activeMinutes) {
        if (activeMinutes < 0) {
            throw new IllegalArgumentException("Active minutes cannot be negative.");
        }
        this.activeMinutes = activeMinutes;
    }

    public void setCaloriesBurned(double caloriesBurned) {
        if (caloriesBurned < 0) {
            throw new IllegalArgumentException("Calories burned cannot be negative.");
        }
        this.caloriesBurned = caloriesBurned;
    }

    public String getActivitySource() {
        return activitySource;
    }

    public void setActivitySource(String activitySource) {
        this.activitySource = activitySource;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
