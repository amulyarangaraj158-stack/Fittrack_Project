package com.fittrack.model;

/**
 * Swimming workout subclass extending Workout.
 * Demonstrates Inheritance and Polymorphic overriding of calculateCalories().
 */
public class Swimming extends Workout {

    public static final String TYPE_NAME = "Swimming";

    public Swimming() {
        super();
    }

    public Swimming(int userId, int durationMinutes, String workoutDate, String intensity,
                    int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        setCaloriesBurned(calculateCalories());
    }

    public Swimming(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                    int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        // MET values for Swimming: Low=5.8, Moderate=7.0, High=9.8
        double met = getIntensityFactor(5.8, 7.0, 9.8);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours + getDistanceKm() * 12.0);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
