package com.fittrack.util;

import com.fittrack.dao.UserDAO;
import com.fittrack.dao.WeightHistoryDAO;
import com.fittrack.model.User;
import com.fittrack.model.WeightEntry;
import com.fittrack.service.FitnessTracker;
import com.fittrack.service.UserService;
import com.fittrack.service.WorkoutService;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Seeds sample users, workouts, water logs, sleep logs, weight trends, goals,
 * workout plans, achievements, and notifications on first startup.
 */
public final class SampleDataSeeder {

    private SampleDataSeeder() {
    }

    public static void seedIfEmpty() throws SQLException {
        UserDAO userDAO = new UserDAO();
        if (userDAO.countUsers() > 0) {
            return;
        }

        UserService userService = new UserService();
        WorkoutService workoutService = new WorkoutService();
        FitnessTracker tracker = new FitnessTracker();
        WeightHistoryDAO weightHistoryDAO = new WeightHistoryDAO();

        User alex = userService.register(
                "Alex Mercer",
                "alex@fittrack.com",
                "FitTrack@123",
                User.ACCOUNT_REGULAR,
                20,
                "Male",
                175.0,
                72.0,
                "Balanced"
        );
        tracker.updateGoals(alex.getUserId(), 8000, 2500, 5, 500.0, 150, 20.0);

        LocalDate today = LocalDate.now();
        workoutService.addWorkout(alex, "Morning Campus Run", "Running", 32, today.toString(), "Moderate", 4950, 4.8, "Steady aerobic pace");
        workoutService.addWorkout(alex, "Mobility Flow", "Yoga", 25, today.toString(), "Low", 0, 0.0, "Hip & shoulder mobility");
        workoutService.addWorkout(alex, "Evening Recovery Walk", "Walking", 40, today.minusDays(1).toString(), "Moderate", 4200, 3.2, "Park loop");
        workoutService.addWorkout(alex, "Upper Body Push/Pull", "Strength Training", 45, today.minusDays(2).toString(), "High", 750, 0.0, "Compound lifts");
        workoutService.addWorkout(alex, "River Trail Ride", "Cycling", 50, today.minusDays(3).toString(), "Moderate", 0, 14.2, "Cadence focus");
        workoutService.addWorkout(alex, "Tabata Intervals", "HIIT", 25, today.minusDays(4).toString(), "High", 1200, 0.0, "20s work / 10s rest");
        workoutService.addWorkout(alex, "Freestyle Pool Laps", "Swimming", 35, today.minusDays(5).toString(), "Moderate", 0, 1.2, "50m intervals");
        workoutService.addWorkout(alex, "Lower Body & Core", "Strength Training", 40, today.minusDays(6).toString(), "Moderate", 600, 0.0, "Squats & planks");
        // Previous period comparison data (8–14 days ago)
        workoutService.addWorkout(alex, "Weekend Park Run", "Running", 28, today.minusDays(9).toString(), "Moderate", 4100, 4.1, "Easy pace");
        workoutService.addWorkout(alex, "Core Circuit", "Custom", 30, today.minusDays(11).toString(), "Moderate", 400, 0.0, "Kettlebell & core");

        workoutService.logDailySteps(alex.getUserId(), 1800, today.toString(), "Daily Steps");
        workoutService.logDailySteps(alex.getUserId(), 2400, today.minusDays(1).toString(), "Daily Steps");

        tracker.logWater(alex.getUserId(), 500, today.toString(), "Morning bottle");
        tracker.logWater(alex.getUserId(), 750, today.toString(), "Post-run hydration");
        tracker.logWater(alex.getUserId(), 500, today.toString(), "Afternoon glass");

        tracker.addSleepLog(alex.getUserId(), today.toString(), 7.5, 4, "Rested well");
        tracker.addSleepLog(alex.getUserId(), today.minusDays(1).toString(), 6.8, 3, "Late study night");
        tracker.addSleepLog(alex.getUserId(), today.minusDays(2).toString(), 8.0, 5, "Deep sleep");
        tracker.addSleepLog(alex.getUserId(), today.minusDays(3).toString(), 7.4, 4, "Consistent bedtime");
        tracker.addSleepLog(alex.getUserId(), today.minusDays(4).toString(), 7.2, 4, "Steady rest");

        weightHistoryDAO.addEntry(new WeightEntry(alex.getUserId(), 73.4, today.minusDays(21).toString()));
        weightHistoryDAO.addEntry(new WeightEntry(alex.getUserId(), 72.9, today.minusDays(14).toString()));
        weightHistoryDAO.addEntry(new WeightEntry(alex.getUserId(), 72.4, today.minusDays(7).toString()));

        tracker.addPlan(alex, "Monday", "Morning 5K Pace", "Running", 30, "Moderate", "Aerobic base");
        tracker.addPlan(alex, "Wednesday", "Compound Lifts", "Strength Training", 45, "High", "Upper & lower");
        tracker.addPlan(alex, "Friday", "Lap Endurance", "Swimming", 35, "Moderate", "Steady breathing");
        tracker.addPlan(alex, "Sunday", "Mobility Reset", "Yoga", 30, "Low", "Active recovery");

        tracker.evaluateAndGetAchievements(alex.getUserId());
        tracker.checkGoalAndReminderNotifications(alex.getUserId());

        User maya = userService.register(
                "Maya Lin",
                "maya@fittrack.com",
                "FitTrack@123",
                User.ACCOUNT_PREMIUM,
                21,
                "Female",
                164.0,
                58.0,
                "Cardio & Endurance"
        );
        tracker.updateGoals(maya.getUserId(), 10000, 3000, 6, 600.0, 210, 28.0);
        workoutService.addWorkout(maya, "Endurance Spin", "Cycling", 45, today.toString(), "High", 0, 16.0, "High cadence climbs");
        workoutService.addWorkout(maya, "Recovery Stretch", "Yoga", 35, today.minusDays(1).toString(), "Moderate", 0, 0.0, "Evening mobility");
        workoutService.addWorkout(maya, "Tempo Intervals", "Running", 40, today.minusDays(2).toString(), "High", 6200, 6.5, "Track intervals");
        workoutService.addWorkout(maya, "Pool Sprint Sets", "Swimming", 35, today.minusDays(4).toString(), "High", 0, 1.4, "Freestyle sprints");
        tracker.logWater(maya.getUserId(), 1000, today.toString(), "Morning flask");
        tracker.logWater(maya.getUserId(), 1000, today.toString(), "Afternoon bottle");
        tracker.addSleepLog(maya.getUserId(), today.toString(), 8.2, 5, "Great recovery");
        tracker.addSleepLog(maya.getUserId(), today.minusDays(1).toString(), 7.8, 4, "Solid rest");
        tracker.addPlan(maya, "Tuesday", "Track Intervals", "Running", 40, "High", "400m repeats");
        tracker.addPlan(maya, "Thursday", "Endurance Ride", "Cycling", 50, "Moderate", "Zone 2 spin");
        tracker.evaluateAndGetAchievements(maya.getUserId());
        tracker.checkGoalAndReminderNotifications(maya.getUserId());
    }
}
