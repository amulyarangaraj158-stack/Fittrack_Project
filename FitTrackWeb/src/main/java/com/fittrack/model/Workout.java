package com.fittrack.model;

import com.fittrack.interfaces.Trackable;

/**
 * Abstract parent Workout class implementing Trackable.
 * Demonstrates Abstraction, Encapsulation, Constructor Overloading, and Method Overloading.
 */
public abstract class Workout implements Trackable {

    public static final double DEFAULT_WEIGHT_KG = 70.0;

    private int workoutId;
    private int userId;
    private String workoutName;
    private int durationMinutes;
    private String workoutDate;
    private String intensity;
    private int steps;
    private double distanceKm;
    private String notes;
    private double caloriesBurned;
    private double userWeightKg;
    private String createdAt;

    public Workout() {
        this.intensity = "Moderate";
        this.userWeightKg = DEFAULT_WEIGHT_KG;
        this.workoutName = getWorkoutType();
    }

    public Workout(int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double userWeightKg) {
        this.userId = userId;
        this.workoutName = getWorkoutType();
        setDurationMinutes(durationMinutes);
        setWorkoutDate(workoutDate);
        setIntensity(intensity);
        setSteps(steps);
        setDistanceKm(distanceKm);
        this.notes = notes == null ? "" : notes.trim();
        this.userWeightKg = userWeightKg > 0 ? userWeightKg : DEFAULT_WEIGHT_KG;
        this.caloriesBurned = calculateCalories();
    }

    public Workout(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        this.workoutId = workoutId;
        this.userId = userId;
        this.workoutName = getWorkoutType();
        this.durationMinutes = durationMinutes;
        this.workoutDate = workoutDate;
        this.intensity = intensity;
        this.steps = steps;
        this.distanceKm = distanceKm;
        this.notes = notes;
        this.caloriesBurned = caloriesBurned;
        this.userWeightKg = DEFAULT_WEIGHT_KG;
        this.createdAt = createdAt;
    }

    public abstract double calculateCalories();

    public abstract String getWorkoutType();

    /**
     * Overloaded method computing calories for an explicit body weight in kg.
     */
    public double calculateCalories(double explicitWeightKg) {
        double previous = this.userWeightKg;
        this.userWeightKg = explicitWeightKg > 0 ? explicitWeightKg : DEFAULT_WEIGHT_KG;
        double result = calculateCalories();
        this.userWeightKg = previous;
        return result;
    }

    protected double getIntensityFactor(double lowMet, double moderateMet, double highMet) {
        if ("Low".equalsIgnoreCase(intensity)) {
            return lowMet;
        } else if ("High".equalsIgnoreCase(intensity)) {
            return highMet;
        }
        return moderateMet;
    }

    protected double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    @Override
    public String trackActivity() {
        String label = (workoutName == null || workoutName.isBlank()) ? getWorkoutType() : workoutName;
        return String.format("%s (%s) · %d min · %.1f kcal (%s)",
                label, getWorkoutType(), durationMinutes, getCaloriesBurned(), workoutDate);
    }

    @Override
    public String getActivityDate() {
        return workoutDate;
    }

    @Override
    public int getDurationMinutes() {
        return durationMinutes;
    }

    @Override
    public double getCaloriesBurned() {
        if (caloriesBurned <= 0 && durationMinutes > 0) {
            caloriesBurned = calculateCalories();
        }
        return caloriesBurned;
    }

    public int getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(int workoutId) {
        this.workoutId = workoutId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getWorkoutName() {
        return (workoutName == null || workoutName.isBlank()) ? getWorkoutType() : workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = (workoutName == null || workoutName.isBlank())
                ? getWorkoutType()
                : workoutName.trim();
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes <= 0 || durationMinutes > 1440) {
            throw new IllegalArgumentException("Duration must be between 1 and 1440 minutes.");
        }
        this.durationMinutes = durationMinutes;
    }

    public String getWorkoutDate() {
        return workoutDate;
    }

    public void setWorkoutDate(String workoutDate) {
        if (workoutDate == null || workoutDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Workout date is required.");
        }
        this.workoutDate = workoutDate.trim();
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = (intensity == null || intensity.isBlank()) ? "Moderate" : intensity.trim();
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("Steps cannot be negative.");
        }
        this.steps = steps;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        if (distanceKm < 0) {
            throw new IllegalArgumentException("Distance cannot be negative.");
        }
        this.distanceKm = distanceKm;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    public void setCaloriesBurned(double caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public double getUserWeightKg() {
        return userWeightKg;
    }

    public void setUserWeightKg(double userWeightKg) {
        this.userWeightKg = userWeightKg > 0 ? userWeightKg : DEFAULT_WEIGHT_KG;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
