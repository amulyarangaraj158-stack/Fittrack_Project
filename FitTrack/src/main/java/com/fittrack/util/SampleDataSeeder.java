package com.fittrack.util;

import com.fittrack.dao.UserDAO;
import com.fittrack.model.User;
import com.fittrack.service.GoalService;
import com.fittrack.service.UserService;
import com.fittrack.service.WaterService;
import com.fittrack.service.WorkoutService;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Populates realistic sample data on first application startup when the database is empty.
 * Allows immediate testing of Dashboard charts, Workout CRUD, Goals, and Water Tracker.
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
        GoalService goalService = new GoalService();
        WaterService waterService = new WaterService();

        // Sample Regular User
        User alex = userService.register(
                "Alex Mercer",
                "alex@fittrack.com",
                "FitTrack@123",
                User.ACCOUNT_REGULAR,
                20,
                "Male",
                175.0,
                72.0
        );

        goalService.updateGoal(alex.getUserId(), 8000, 2500, 5, 500.0);

        LocalDate today = LocalDate.now();
        workoutService.addWorkout(alex, "Running", 35, today.toString(), "Moderate", 5400, 5.2, "Morning campus run");
        workoutService.addWorkout(alex, "Yoga", 25, today.toString(), "Low", 0, 0.0, "Post-run stretching");
        workoutService.addWorkout(alex, "Walking", 40, today.minusDays(1).toString(), "Moderate", 4200, 3.1, "Evening brisk walk");
        workoutService.addWorkout(alex, "Strength Training", 45, today.minusDays(2).toString(), "High", 800, 0.0, "Upper body push/pull");
        workoutService.addWorkout(alex, "Cycling", 50, today.minusDays(3).toString(), "Moderate", 0, 14.5, "Lake trail ride");
        workoutService.addWorkout(alex, "Running", 30, today.minusDays(4).toString(), "High", 4900, 4.8, "Interval sprints");
        workoutService.addWorkout(alex, "Walking", 55, today.minusDays(5).toString(), "Low", 5800, 4.0, "Park walk");
        workoutService.addWorkout(alex, "Strength Training", 40, today.minusDays(6).toString(), "Moderate", 600, 0.0, "Leg day & core");

        workoutService.logDailySteps(alex.getUserId(), 1450, today.toString());

        waterService.logWater(alex.getUserId(), 500, today.toString(), "Morning hydration");
        waterService.logWater(alex.getUserId(), 750, today.toString(), "Post-run bottle");
        waterService.logWater(alex.getUserId(), 500, today.toString(), "Afternoon glass");

        // Sample Premium User
        User maya = userService.register(
                "Maya Lin",
                "maya@fittrack.com",
                "FitTrack@123",
                User.ACCOUNT_PREMIUM,
                21,
                "Female",
                164.0,
                58.0
        );
        goalService.updateGoal(maya.getUserId(), 10000, 3000, 6, 600.0);
        workoutService.addWorkout(maya, "Cycling", 45, today.toString(), "High", 0, 16.0, "Spin class");
        workoutService.addWorkout(maya, "Yoga", 40, today.minusDays(1).toString(), "Moderate", 0, 0.0, "Vinyasa flow");
        waterService.logWater(maya.getUserId(), 1000, today.toString(), "Morning flask");
    }
}
