package com.fittrack.model;

/**
 * Walking workout subclass.
 * Demonstrates Inheritance and Polymorphic overriding of calculateCalories().
 */
public class Walking extends Workout {

    public static final String TYPE_NAME = "Walking";

    public Walking() {
        super();
    }

    public Walking(int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        if (getSteps() == 0) {
            // Estimate ~100 steps per minute if user did not manually enter steps
            setSteps(durationMinutes * 100);
        }
        setCaloriesBurned(calculateCalories());
    }

    public Walking(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        // MET values for Walking: Low=2.8, Moderate=3.5, High=4.5
        double met = getIntensityFactor(2.8, 3.5, 4.5);
        double hours = getDurationMinutes() / 60.0;
        double baseCalories = met * getUserWeightKg() * hours;
        double distanceBonus = getDistanceKm() * 4.0;
        return roundOneDecimal(baseCalories + distanceBonus);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
