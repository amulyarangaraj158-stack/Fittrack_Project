package com.fittrack.service;

import com.fittrack.dao.ActivityRecordDAO;
import com.fittrack.dao.WorkoutDAO;
import com.fittrack.model.ActivityRecord;
import com.fittrack.model.CustomWorkout;
import com.fittrack.model.Cycling;
import com.fittrack.model.HIIT;
import com.fittrack.model.Running;
import com.fittrack.model.StrengthTraining;
import com.fittrack.model.Swimming;
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
 * Service layer for Workout CRUD, polymorphic calorie calculation, activity logging, and period analytics.
 */
public class WorkoutService {

    private final WorkoutDAO workoutDAO;
    private final ActivityRecordDAO activityRecordDAO;

    public WorkoutService() {
        this.workoutDAO = new WorkoutDAO();
        this.activityRecordDAO = new ActivityRecordDAO();
    }

    /**
     * Instantiates the concrete Workout subclass via a Workout reference (Runtime Polymorphism).
     */
    public Workout createWorkoutInstance(String workoutName, String type, int userId, int durationMinutes,
                                         String workoutDate, String intensity, int steps,
                                         double distanceKm, String notes, double userWeightKg) {
        if (!ValidationUtils.isValidDate(workoutDate)) {
            throw new IllegalArgumentException("Workout date must be in YYYY-MM-DD format.");
        }
        Workout workout = switch (type) {
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
            case Swimming.TYPE_NAME ->
                    new Swimming(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            case HIIT.TYPE_NAME ->
                    new HIIT(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            case CustomWorkout.TYPE_NAME ->
                    new CustomWorkout(userId, durationMinutes, workoutDate, intensity, steps, distanceKm, notes, userWeightKg);
            default -> throw new IllegalArgumentException("Unsupported workout type: " + type);
        };
        workout.setWorkoutName(workoutName);
        return workout;
    }

    /**
     * Overloaded helper preserving compatibility for callers without a custom workoutName.
     */
    public Workout createWorkoutInstance(String type, int userId, int durationMinutes, String workoutDate,
                                         String intensity, int steps, double distanceKm, String notes,
                                         double userWeightKg) {
        return createWorkoutInstance(type, type, userId, durationMinutes, workoutDate,
                intensity, steps, distanceKm, notes, userWeightKg);
    }

    public double previewCalories(String type, int durationMinutes, String intensity,
                                  int steps, double distanceKm, double userWeightKg) {
        Workout workout = createWorkoutInstance(
                type, type, 0, durationMinutes, LocalDate.now().toString(),
                intensity, steps, distanceKm, "", userWeightKg
        );
        return workout.calculateCalories(userWeightKg);
    }

    public Workout addWorkout(User user, String workoutName, String type, int durationMinutes, String workoutDate,
                              String intensity, int steps, double distanceKm, String notes) throws SQLException {
        Workout workout = createWorkoutInstance(
                workoutName, type, user.getUserId(), durationMinutes, workoutDate,
                intensity, steps, distanceKm, notes, user.getWeightKg()
        );
        return workoutDAO.addWorkout(workout);
    }

    public Workout addWorkout(User user, String type, int durationMinutes, String workoutDate,
                              String intensity, int steps, double distanceKm, String notes) throws SQLException {
        return addWorkout(user, type, type, durationMinutes, workoutDate, intensity, steps, distanceKm, notes);
    }

    public boolean updateWorkout(User user, int workoutId, String workoutName, String type, int durationMinutes,
                                 String workoutDate, String intensity, int steps, double distanceKm, String notes) throws SQLException {
        Workout workout = createWorkoutInstance(
                workoutName, type, user.getUserId(), durationMinutes, workoutDate,
                intensity, steps, distanceKm, notes, user.getWeightKg()
        );
        workout.setWorkoutId(workoutId);
        return workoutDAO.updateWorkout(workout);
    }

    public boolean deleteWorkout(int workoutId, int userId) throws SQLException {
        return workoutDAO.deleteWorkout(workoutId, userId);
    }

    public Workout getWorkoutById(int workoutId, int userId) throws SQLException {
        return workoutDAO.findById(workoutId, userId);
    }

    public ArrayList<Workout> getAllWorkouts(int userId) throws SQLException {
        return workoutDAO.findAllByUser(userId);
    }

    public ArrayList<Workout> getFilteredWorkouts(int userId, String startDate, String endDate, String query) throws SQLException {
        return workoutDAO.findByUserFiltered(userId, startDate, endDate, query);
    }

    public ArrayList<Workout> getFilteredSortedWorkouts(int userId, String startDate, String endDate,
                                                        String typeFilter, String intensityFilter,
                                                        String query, String sortBy, String sortDir) throws SQLException {
        return workoutDAO.findByUserFilteredAndSorted(
                userId, startDate, endDate, typeFilter, intensityFilter, query, sortBy, sortDir);
    }

    public ArrayList<Workout> getWorkoutsLast7Days(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return workoutDAO.findByUserBetweenDates(userId, today.minusDays(6).toString(), today.toString());
    }

    public ArrayList<Workout> getWorkoutsBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        return workoutDAO.findByUserBetweenDates(userId, startDate, endDate);
    }

    public Map<String, Object> getSummaryBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        return workoutDAO.getSummaryBetweenDates(userId, startDate, endDate);
    }

    public ActivityRecord logDailySteps(int userId, int stepsCount, String date, String sourceLabel) throws SQLException {
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
        }
        if (stepsCount <= 0 || stepsCount > 200000) {
            throw new IllegalArgumentException("Steps must be between 1 and 200,000.");
        }
        double estimatedCalories = Math.round((stepsCount * 0.04) * 10.0) / 10.0;
        int minutes = Math.max(1, stepsCount / 100);
        String source = (sourceLabel == null || sourceLabel.isBlank()) ? "Daily Steps" : sourceLabel.trim();
        ActivityRecord record = new ActivityRecord(
                userId, null, date, stepsCount, minutes, estimatedCalories, source
        );
        return activityRecordDAO.addActivityRecord(record);
    }

    public ActivityRecord logDailySteps(int userId, int stepsCount, String date) throws SQLException {
        return logDailySteps(userId, stepsCount, date, "Daily Steps");
    }

    public ArrayList<ActivityRecord> getRecentActivityRecords(int userId) throws SQLException {
        return activityRecordDAO.findRecentByUser(userId, 50);
    }

    public boolean deleteManualActivityRecord(int recordId, int userId) throws SQLException {
        return activityRecordDAO.deleteManualRecord(recordId, userId);
    }

    public int getTodaySteps(int userId) throws SQLException {
        return activityRecordDAO.getTotalStepsByDate(userId, LocalDate.now().toString());
    }

    public int getWeeklySteps(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return activityRecordDAO.getTotalStepsBetweenDates(userId, today.minusDays(6).toString(), today.toString());
    }

    public int getStepsBetweenDates(int userId, String startDate, String endDate) throws SQLException {
        return activityRecordDAO.getTotalStepsBetweenDates(userId, startDate, endDate);
    }

    public Map<String, Integer> getDailyStepsSeries(int userId, LocalDate start, LocalDate end) throws SQLException {
        Map<String, Integer> raw = activityRecordDAO.getDailyStepsBetweenDates(userId, start.toString(), end.toString());
        Map<String, Integer> filled = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = cursor.toString();
            filled.put(key, raw.getOrDefault(key, 0));
            cursor = cursor.plusDays(1);
        }
        return filled;
    }

    public double getTodayCalories(int userId) throws SQLException {
        return workoutDAO.getTotalCaloriesByDate(userId, LocalDate.now().toString());
    }

    public int getTodayDuration(int userId) throws SQLException {
        return workoutDAO.getTotalDurationByDate(userId, LocalDate.now().toString());
    }

    public int getWeeklyWorkoutCount(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return workoutDAO.countWorkoutsBetweenDates(userId, today.minusDays(6).toString(), today.toString());
    }

    public int countDistinctTypes(int userId) throws SQLException {
        return workoutDAO.countDistinctTypesByUser(userId);
    }

    public ArrayList<String> getActiveDates(int userId) throws SQLException {
        return workoutDAO.findActiveDatesByUser(userId);
    }

    public Map<String, Double> getLast7DaysCalories(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return getDailyCaloriesBetween(userId, today.minusDays(6), today);
    }

    public Map<String, Integer> getLast7DaysDuration(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return getDailyDurationBetween(userId, today.minusDays(6), today);
    }

    public Map<String, Double> getDailyCaloriesBetween(int userId, LocalDate start, LocalDate end) throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            map.put(cursor.toString(), 0.0);
            cursor = cursor.plusDays(1);
        }
        for (Workout w : workoutDAO.findByUserBetweenDates(userId, start.toString(), end.toString())) {
            map.computeIfPresent(w.getWorkoutDate(), (k, v) -> Math.round((v + w.getCaloriesBurned()) * 10.0) / 10.0);
        }
        return map;
    }

    public Map<String, Integer> getDailyDurationBetween(int userId, LocalDate start, LocalDate end) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            map.put(cursor.toString(), 0);
            cursor = cursor.plusDays(1);
        }
        for (Workout w : workoutDAO.findByUserBetweenDates(userId, start.toString(), end.toString())) {
            map.computeIfPresent(w.getWorkoutDate(), (k, v) -> v + w.getDurationMinutes());
        }
        return map;
    }

    public Map<String, Integer> getDailySessionCountBetween(int userId, LocalDate start, LocalDate end) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            map.put(cursor.toString(), 0);
            cursor = cursor.plusDays(1);
        }
        for (Workout w : workoutDAO.findByUserBetweenDates(userId, start.toString(), end.toString())) {
            map.computeIfPresent(w.getWorkoutDate(), (k, v) -> v + 1);
        }
        return map;
    }
}
