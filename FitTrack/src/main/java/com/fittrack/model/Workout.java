package com.fittrack.model;

import com.fittrack.interfaces.Trackable;

/**
 * Abstract parent class representing any workout activity in FitTrack.
 *
 * Demonstrates:
 * - Abstraction (abstract class + abstract calculateCalories() and getWorkoutType())
 * - Interface implementation (implements Trackable)
 * - Encapsulation (private fields with getters and setters)
 * - Constructor Overloading & Method Overloading
 */
public abstract class Workout implements Trackable {

    public static final double DEFAULT_WEIGHT_KG = 70.0;

    private int workoutId;
    private int userId;
    private int durationMinutes;
    private String workoutDate;
    private String intensity;
    private int steps;
    private double distanceKm;
    private String notes;
    private double caloriesBurned;
    private double userWeightKg;
    private String createdAt;

    // Default constructor
    public Workout() {
        this.intensity = "Moderate";
        this.userWeightKg = DEFAULT_WEIGHT_KG;
    }

    // Overloaded constructor for creating a new workout
    public Workout(int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double userWeightKg) {
        this.userId = userId;
        setDurationMinutes(durationMinutes);
        setWorkoutDate(workoutDate);
        setIntensity(intensity);
        setSteps(steps);
        setDistanceKm(distanceKm);
        this.notes = notes == null ? "" : notes.trim();
        this.userWeightKg = userWeightKg > 0 ? userWeightKg : DEFAULT_WEIGHT_KG;
        this.caloriesBurned = calculateCalories();
    }

    // Overloaded full constructor for loading from SQLite database
    public Workout(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        this.workoutId = workoutId;
        this.userId = userId;
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

    /**
     * Abstract method overridden by every concrete workout subclass
     * to compute calories burned based on activity-specific physiology.
     */
    public abstract double calculateCalories();

    /**
     * Abstract method returning the canonical workout type name.
     */
    public abstract String getWorkoutType();

    /**
     * Overloaded method: computes calories for an explicit body weight in kg.
     */
    public double calculateCalories(double explicitWeightKg) {
        double previousWeight = this.userWeightKg;
        this.userWeightKg = explicitWeightKg > 0 ? explicitWeightKg : DEFAULT_WEIGHT_KG;
        double computed = calculateCalories();
        this.userWeightKg = previousWeight;
        return computed;
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
        return String.format("%s (%d min, %s intensity) on %s -> %.1f kcal",
                getWorkoutType(), durationMinutes, intensity, workoutDate, getCaloriesBurned());
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

    // Encapsulated Getters and Setters
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
            throw new IllegalArgumentException("Workout date cannot be empty.");
        }
        this.workoutDate = workoutDate.trim();
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        if (intensity == null || intensity.isBlank()) {
            this.intensity = "Moderate";
        } else {
            this.intensity = intensity.trim();
        }
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
