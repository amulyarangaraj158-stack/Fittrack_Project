package com.fittrack.model;

/**
 * CustomWorkout subclass extending Workout for user-defined training modalities.
 * Demonstrates Inheritance and Polymorphic overriding of calculateCalories().
 */
public class CustomWorkout extends Workout {

    public static final String TYPE_NAME = "Custom";

    public CustomWorkout() {
        super();
    }

    public CustomWorkout(int userId, int durationMinutes, String workoutDate, String intensity,
                         int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        setCaloriesBurned(calculateCalories());
    }

    public CustomWorkout(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                         int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        // Flexible MET values for Custom workouts: Low=4.2, Moderate=6.2, High=8.6
        double met = getIntensityFactor(4.2, 6.2, 8.6);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours + getDistanceKm() * 4.5);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
