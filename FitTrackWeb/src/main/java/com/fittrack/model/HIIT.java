package com.fittrack.model;

/**
 * HIIT (High-Intensity Interval Training) workout subclass extending Workout.
 * Demonstrates Inheritance and Polymorphic overriding of calculateCalories().
 */
public class HIIT extends Workout {

    public static final String TYPE_NAME = "HIIT";
    private static final double EPOC_MULTIPLIER = 1.12;

    public HIIT() {
        super();
    }

    public HIIT(int userId, int durationMinutes, String workoutDate, String intensity,
                int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        setCaloriesBurned(calculateCalories());
    }

    public HIIT(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        // MET values for HIIT: Low=6.5, Moderate=8.5, High=11.0 (+12% EPOC)
        double met = getIntensityFactor(6.5, 8.5, 11.0);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours * EPOC_MULTIPLIER);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
