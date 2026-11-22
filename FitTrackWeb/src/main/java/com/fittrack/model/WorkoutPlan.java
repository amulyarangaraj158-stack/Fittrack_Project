package com.fittrack.model;

/**
 * Represents a scheduled weekly workout plan entry.
 */
public class WorkoutPlan {

    private int planId;
    private int userId;
    private String dayOfWeek;
    private String workoutName;
    private String workoutType;
    private int plannedMinutes;
    private String intensity;
    private String notes;
    private String completedDate;

    public WorkoutPlan() {
    }

    public WorkoutPlan(int userId, String dayOfWeek, String workoutType,
                       int plannedMinutes, String intensity, String notes) {
        this(userId, dayOfWeek, workoutType, workoutType, plannedMinutes, intensity, notes);
    }

    public WorkoutPlan(int userId, String dayOfWeek, String workoutName, String workoutType,
                       int plannedMinutes, String intensity, String notes) {
        this.userId = userId;
        setDayOfWeek(dayOfWeek);
        setWorkoutType(workoutType);
        setWorkoutName(workoutName);
        setPlannedMinutes(plannedMinutes);
        this.intensity = (intensity == null || intensity.isBlank()) ? "Moderate" : intensity.trim();
        this.notes = notes == null ? "" : notes.trim();
    }

    public WorkoutPlan(int planId, int userId, String dayOfWeek, String workoutName, String workoutType,
                       int plannedMinutes, String intensity, String notes, String completedDate) {
        this.planId = planId;
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.workoutType = workoutType;
        this.workoutName = (workoutName == null || workoutName.isBlank()) ? workoutType : workoutName;
        this.plannedMinutes = plannedMinutes;
        this.intensity = intensity;
        this.notes = notes;
        this.completedDate = completedDate;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek.isBlank()) {
            throw new IllegalArgumentException("Day of week is required.");
        }
        this.dayOfWeek = dayOfWeek.trim();
    }

    public String getWorkoutName() {
        return (workoutName == null || workoutName.isBlank()) ? workoutType : workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = (workoutName == null || workoutName.isBlank())
                ? workoutType
                : workoutName.trim();
    }

    public String getWorkoutType() {
        return workoutType;
    }

    public void setWorkoutType(String workoutType) {
        if (workoutType == null || workoutType.isBlank()) {
            throw new IllegalArgumentException("Workout type is required.");
        }
        this.workoutType = workoutType.trim();
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public void setPlannedMinutes(int plannedMinutes) {
        if (plannedMinutes <= 0 || plannedMinutes > 600) {
            throw new IllegalArgumentException("Planned duration must be between 1 and 600 minutes.");
        }
        this.plannedMinutes = plannedMinutes;
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = (intensity == null || intensity.isBlank()) ? "Moderate" : intensity.trim();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    public String getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(String completedDate) {
        this.completedDate = completedDate;
    }
}
