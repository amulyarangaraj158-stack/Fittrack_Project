package com.fittrack.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite JDBC connections, table initialization, and safe non-destructive schema migrations.
 */
public final class DatabaseConnection {

    private static final String DB_FOLDER = "database";
    private static final String DB_FILE = "fittrack_web.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FOLDER + File.separator + DB_FILE;

    private static volatile boolean initialized = false;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        File dir = new File(DB_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static synchronized void initializeDatabase() throws SQLException {
        if (initialized) {
            return;
        }

        String usersTable = """
                CREATE TABLE IF NOT EXISTS Users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    account_type TEXT NOT NULL DEFAULT 'REGULAR',
                    age INTEGER NOT NULL DEFAULT 20,
                    gender TEXT NOT NULL DEFAULT 'Not Specified',
                    height_cm REAL NOT NULL DEFAULT 170.0,
                    weight_kg REAL NOT NULL DEFAULT 70.0,
                    activity_preference TEXT NOT NULL DEFAULT 'Balanced',
                    created_at TEXT NOT NULL
                );
                """;

        String workoutsTable = """
                CREATE TABLE IF NOT EXISTS Workouts (
                    workout_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    workout_name TEXT,
                    workout_type TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    calories_burned REAL NOT NULL,
                    workout_date TEXT NOT NULL,
                    intensity TEXT NOT NULL DEFAULT 'Moderate',
                    steps INTEGER NOT NULL DEFAULT 0,
                    distance_km REAL NOT NULL DEFAULT 0.0,
                    notes TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String goalsTable = """
                CREATE TABLE IF NOT EXISTS FitnessGoals (
                    goal_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL UNIQUE,
                    daily_step_target INTEGER NOT NULL DEFAULT 8000,
                    daily_water_target_ml INTEGER NOT NULL DEFAULT 2500,
                    weekly_workout_target INTEGER NOT NULL DEFAULT 5,
                    daily_calorie_target REAL NOT NULL DEFAULT 500.0,
                    active_minutes_target INTEGER NOT NULL DEFAULT 150,
                    distance_km_target REAL NOT NULL DEFAULT 20.0,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String goalHistoryTable = """
                CREATE TABLE IF NOT EXISTS GoalHistory (
                    history_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    daily_step_target INTEGER NOT NULL,
                    daily_water_target_ml INTEGER NOT NULL,
                    weekly_workout_target INTEGER NOT NULL,
                    daily_calorie_target REAL NOT NULL,
                    active_minutes_target INTEGER NOT NULL,
                    distance_km_target REAL NOT NULL,
                    recorded_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String waterTable = """
                CREATE TABLE IF NOT EXISTS WaterIntake (
                    water_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    amount_ml INTEGER NOT NULL,
                    intake_date TEXT NOT NULL,
                    intake_time TEXT NOT NULL,
                    note TEXT,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String activityTable = """
                CREATE TABLE IF NOT EXISTS ActivityRecords (
                    record_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    workout_id INTEGER,
                    activity_date TEXT NOT NULL,
                    steps_count INTEGER NOT NULL DEFAULT 0,
                    active_minutes INTEGER NOT NULL DEFAULT 0,
                    calories_burned REAL NOT NULL DEFAULT 0.0,
                    activity_source TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
                    FOREIGN KEY (workout_id) REFERENCES Workouts(workout_id) ON DELETE CASCADE
                );
                """;

        String achievementsTable = """
                CREATE TABLE IF NOT EXISTS UserAchievements (
                    achievement_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    badge_code TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    unlocked_at TEXT NOT NULL,
                    UNIQUE (user_id, badge_code),
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String plansTable = """
                CREATE TABLE IF NOT EXISTS WorkoutPlans (
                    plan_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    day_of_week TEXT NOT NULL,
                    workout_name TEXT,
                    workout_type TEXT NOT NULL,
                    planned_minutes INTEGER NOT NULL,
                    intensity TEXT NOT NULL DEFAULT 'Moderate',
                    notes TEXT,
                    completed_date TEXT,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String sleepTable = """
                CREATE TABLE IF NOT EXISTS SleepLogs (
                    sleep_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    sleep_date TEXT NOT NULL,
                    hours_slept REAL NOT NULL,
                    quality_rating INTEGER NOT NULL,
                    notes TEXT,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String weightHistoryTable = """
                CREATE TABLE IF NOT EXISTS WeightHistory (
                    weight_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    weight_kg REAL NOT NULL,
                    recorded_date TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String notificationsTable = """
                CREATE TABLE IF NOT EXISTS Notifications (
                    notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    is_read INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String userSettingsTable = """
                CREATE TABLE IF NOT EXISTS UserSettings (
                    setting_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL UNIQUE,
                    theme_mode TEXT NOT NULL DEFAULT 'light',
                    workout_reminders INTEGER NOT NULL DEFAULT 1,
                    water_reminders INTEGER NOT NULL DEFAULT 1,
                    goal_alerts INTEGER NOT NULL DEFAULT 1,
                    achievement_alerts INTEGER NOT NULL DEFAULT 1,
                    weekly_digest INTEGER NOT NULL DEFAULT 1,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        String workoutStreaksTable = """
                CREATE TABLE IF NOT EXISTS WorkoutStreaks (
                    streak_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL UNIQUE,
                    current_streak INTEGER NOT NULL DEFAULT 0,
                    longest_streak INTEGER NOT NULL DEFAULT 0,
                    last_active_date TEXT,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(workoutsTable);
            stmt.execute(goalsTable);
            stmt.execute(goalHistoryTable);
            stmt.execute(waterTable);
            stmt.execute(activityTable);
            stmt.execute(achievementsTable);
            stmt.execute(plansTable);
            stmt.execute(sleepTable);
            stmt.execute(weightHistoryTable);
            stmt.execute(notificationsTable);
            stmt.execute(userSettingsTable);
            stmt.execute(workoutStreaksTable);

            // Safe, non-destructive column migrations for existing databases
            ensureColumnExists(conn, "Workouts", "workout_name", "TEXT");
            ensureColumnExists(conn, "FitnessGoals", "active_minutes_target", "INTEGER NOT NULL DEFAULT 150");
            ensureColumnExists(conn, "FitnessGoals", "distance_km_target", "REAL NOT NULL DEFAULT 20.0");
            ensureColumnExists(conn, "WorkoutPlans", "workout_name", "TEXT");

            initialized = true;
        }
    }

    private static void ensureColumnExists(Connection conn, String tableName, String columnName, String columnDef)
            throws SQLException {
        boolean found = false;
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + tableName + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            try (Statement alter = conn.createStatement()) {
                alter.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef);
            }
        }
    }
}
