package com.fittrack.model;

/**
 * Running workout subclass overriding calculateCalories().
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
        double met = getIntensityFactor(7.0, 9.8, 11.5);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours + getDistanceKm() * 8.0);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
