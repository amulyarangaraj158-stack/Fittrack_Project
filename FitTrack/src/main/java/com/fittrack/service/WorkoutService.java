package com.fittrack.service;

import com.fittrack.dao.ActivityRecordDAO;
import com.fittrack.dao.WorkoutDAO;
import com.fittrack.model.ActivityRecord;
import com.fittrack.model.Cycling;
import com.fittrack.model.Running;
import com.fittrack.model.StrengthTraining;
import com.fittrack.model.User;
import com.fittrack.model.Walking;
import com.fittrack.model.Workout;
import com.fittrack.model.Yoga;
import com.fittrack.util.ValidationUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business logic layer for Workouts and Daily Activity tracking.
 * Demonstrates runtime polymorphism using abstract Workout references.
 */
public class WorkoutService {

    public static final String[] WORKOUT_TYPES = {
            Walking.TYPE_NAME,
            Running.TYPE_NAME,
            Cycling.TYPE_NAME,
            Yoga.TYPE_NAME,
            StrengthTraining.TYPE_NAME
    };

    public static final String[] INTENSITY_LEVELS = {"Low", "Moderate", "High"};

    private final WorkoutDAO workoutDAO;
    private final ActivityRecordDAO activityRecordDAO;

    public WorkoutService() {
        this.workoutDAO = new WorkoutDAO();
        this.activityRecordDAO = new ActivityRecordDAO();
    }

    /**
     * Factory method returning a concrete subclass via a base Workout reference.
     * Demonstrates Runtime Polymorphism.
     */
    public Workout buildWorkout(String workoutType, int userId, int durationMinutes, String workoutDate,
                                String intensity, int steps, double distanceKm, String notes, double userWeightKg) {
        if (!ValidationUtils.isValidDate(workoutDate)) {
            throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
        }
        return switch (workoutType) {
            case Walking.TYPE_NAME ->
                    new Walking(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            case Running.TYPE_NAME ->
                    new Running(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            case Cycling.TYPE_NAME ->
                    new Cycling(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            case Yoga.TYPE_NAME ->
                    new Yoga(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            case StrengthTraining.TYPE_NAME ->
                    new StrengthTraining(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            default -> throw new IllegalArgumentException("Unsupported workout type: " + workoutType);
        };
    }

    /**
     * Demonstrates runtime polymorphism by invoking calculateCalories() on a Workout reference.
     */
    public double previewCalories(String workoutType, int durationMinutes, String intensity,
                                  int steps, double distanceKm, double userWeightKg) {
        Workout workout = buildWorkout(
                workoutType, 0, durationMinutes, LocalDate.now().toString(),
                intensity, steps, distanceKm, "", userWeightKg
        );
        return workout.calculateCalories(userWeightKg);
    }

    public Workout addWorkout(User user, String workoutType, int durationMinutes, String workoutDate,
                              String intensity, int steps, double distanceKm, String notes) throws SQLException {
        Workout workout = buildWorkout(
                workoutType, user.getUserId(), durationMinutes, workoutDate,
                intensity, steps, distanceKm, notes, user.getWeightKg()
        );
        return workoutDAO.addWorkout(workout);
    }

    public boolean updateWorkout(User user, int workoutId, String workoutType, int durationMinutes, String workoutDate,
                                 String intensity, int steps, double distanceKm, String notes) throws SQLException {
        Workout workout = buildWorkout(
                workoutType, user.getUserId(), durationMinutes, workoutDate,
                intensity, steps, distanceKm, notes, user.getWeightKg()
        );
        workout.setWorkoutId(workoutId);
        return workoutDAO.updateWorkout(workout);
    }

    public boolean deleteWorkout(int workoutId, int userId) throws SQLException {
        return workoutDAO.deleteWorkout(workoutId, userId);
    }

    public ArrayList<Workout> getAllWorkouts(int userId) throws SQLException {
        return workoutDAO.findAllByUser(userId);
    }

    public ArrayList<Workout> getWorkoutsByType(int userId, String workoutType) throws SQLException {
        if ("All Types".equalsIgnoreCase(workoutType)) {
            return workoutDAO.findAllByUser(userId);
        }
        return workoutDAO.findByUserAndType(userId, workoutType);
    }

    public ArrayList<Workout> getWorkoutsLastNDays(int userId, int days) throws SQLException {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(0, days - 1));
        return workoutDAO.findByUserBetweenDates(userId, start.toString(), end.toString());
    }

    public ActivityRecord logDailySteps(int userId, int stepsCount, String date) throws SQLException {
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
        }
        if (stepsCount <= 0) {
            throw new IllegalArgumentException("Steps must be greater than 0.");
        }
        double estimatedCalories = Math.round((stepsCount * 0.04) * 10.0) / 10.0;
        int estimatedMinutes = Math.max(1, stepsCount / 100);
        ActivityRecord record = new ActivityRecord(
                userId, null, date, stepsCount, estimatedMinutes, estimatedCalories, "Daily Steps"
        );
        return activityRecordDAO.addActivityRecord(record);
    }

    public int getTodaySteps(int userId) throws SQLException {
        return activityRecordDAO.getTotalStepsByDate(userId, LocalDate.now().toString());
    }

    public double getTodayCalories(int userId) throws SQLException {
        return workoutDAO.getTotalCaloriesByDate(userId, LocalDate.now().toString());
    }

    public int getTodayDuration(int userId) throws SQLException {
        return workoutDAO.getTotalDurationByDate(userId, LocalDate.now().toString());
    }

    public int getWeeklyWorkoutCount(int userId) throws SQLException {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        return workoutDAO.countWorkoutsBetweenDates(userId, start.toString(), end.toString());
    }

    public int getWeeklyTotalSteps(int userId) throws SQLException {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        return activityRecordDAO.getTotalStepsBetweenDates(userId, start.toString(), end.toString());
    }

    /**
     * Returns ordered 7-day calorie totals keyed by ISO date string (oldest to newest).
     */
    public Map<String, Double> getLast7DaysCaloriesMap(int userId) throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            map.put(today.minusDays(i).toString(), 0.0);
        }
        ArrayList<Workout> recent = getWorkoutsLastNDays(userId, 7);
        for (Workout w : recent) {
            map.computeIfPresent(w.getWorkoutDate(), (k, v) -> Math.round((v + w.getCaloriesBurned()) * 10.0) / 10.0);
        }
        return map;
    }

    /**
     * Returns ordered 7-day duration totals (minutes) keyed by ISO date string.
     */
    public Map<String, Integer> getLast7DaysDurationMap(int userId) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            map.put(today.minusDays(i).toString(), 0);
        }
        ArrayList<Workout> recent = getWorkoutsLastNDays(userId, 7);
        for (Workout w : recent) {
            map.computeIfPresent(w.getWorkoutDate(), (k, v) -> v + w.getDurationMinutes());
        }
        return map;
    }
}
