package com.fittrack.model;

/**
 * Running workout subclass.
 * Demonstrates Inheritance and Polymorphic overriding of calculateCalories().
 */
public class Running extends Workout {

    public static final String TYPE_NAME = "Running";

    public Running() {
        super();
    }

    public Running(int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        if (getSteps() == 0) {
            // Estimate ~155 steps per minute for running if not entered
            setSteps(durationMinutes * 155);
        }
        setCaloriesBurned(calculateCalories());
    }

    public Running(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                   int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        // MET values for Running: Low=7.0, Moderate=9.8, High=11.5
        double met = getIntensityFactor(7.0, 9.8, 11.5);
        double hours = getDurationMinutes() / 60.0;
        double baseCalories = met * getUserWeightKg() * hours;
        double distanceBonus = getDistanceKm() * 8.0;
        return roundOneDecimal(baseCalories + distanceBonus);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
