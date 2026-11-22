package com.fittrack.model;

/**
 * Cycling workout subclass overriding calculateCalories().
 */
public class Cycling extends Workout {

    public static final String TYPE_NAME = "Cycling";

    public Cycling() {
        super();
    }

    public Cycling(int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        setCaloriesBurned(calculateCalories());
    }

    public Cycling(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        double met = getIntensityFactor(4.5, 7.5, 10.0);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours + getDistanceKm() * 3.5);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
