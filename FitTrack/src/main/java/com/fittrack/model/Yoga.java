package com.fittrack.model;

/**
 * Yoga workout subclass.
 * Demonstrates Inheritance and Polymorphic overriding of calculateCalories().
 */
public class Yoga extends Workout {

    public static final String TYPE_NAME = "Yoga";

    public Yoga() {
        super();
    }

    public Yoga(int userId, int durationMinutes, String workoutDate, String intensity,
                int steps, double distanceKm, String notes, double userWeightKg) {
        super(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
        setCaloriesBurned(calculateCalories());
    }

    public Yoga(int workoutId, int userId, int durationMinutes, String workoutDate, String intensity,
                int steps, double distanceKm, String notes, double caloriesBurned, String createdAt) {
        super(workoutId, userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, caloriesBurned, createdAt);
    }

    @Override
    public double calculateCalories() {
        // MET values for Yoga: Low=2.3 (Hatha), Moderate=3.0 (Vinyasa), High=4.0 (Power Yoga)
        double met = getIntensityFactor(2.3, 3.0, 4.0);
        double hours = getDurationMinutes() / 60.0;
        return roundOneDecimal(met * getUserWeightKg() * hours);
    }

    @Override
    public String getWorkoutType() {
        return TYPE_NAME;
    }
}
