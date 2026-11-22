package com.fittrack.model;

/**
 * Walking workout subclass overriding calculateCalories().
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
        double met = getIntensityFactor(2.8, 3.5, 4.5);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours + getDistanceKm() * 4.0);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
