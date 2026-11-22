package com.fittrack.model;

/**
 * StrengthTraining workout subclass.
 * Demonstrates Inheritance and Polymorphic overriding of calculateCalories().
 */
public class StrengthTraining extends Workout {

    public static final String TYPE_NAME = "Strength Training";
    private static final double AFTERBURN_MULTIPLIER = 1.08;

    public StrengthTraining() {
        super();
    }

    public StrengthTraining(int userId, int durationMinutes, String workoutDate, String intensity,
                            int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        setCaloriesBurned(calculateCalories());
    }

    public StrengthTraining(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                            int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        // MET values for Strength Training: Low=3.5, Moderate=5.0, High=6.5 (+ 8% EPOC factor)
        double met = getIntensityFactor(3.5, 5.0, 6.5);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours * AFTERBURN_MULTIPLIER);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
