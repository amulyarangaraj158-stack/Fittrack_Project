package com.fittrack.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite JDBC connections and automatic database/table initialization.
 */
public final class DatabaseConnection {

    private static final String DB_FOLDER = "database";
    private static final String DB_FILE = "fittrack.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FOLDER + File.separator + DB_FILE;

    private static volatile boolean initialized = false;

    private DatabaseConnection() {
        // Private constructor prevents instantiation of utility class
    }

    /**
     * Opens a new SQLite JDBC connection with foreign key enforcement enabled.
     */
    public static Connection getConnection() throws SQLException {
        ensureDatabaseDirectoryExists();
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Creates the database directory and all required tables if they do not exist.
     */
    public static synchronized void initializeDatabase() throws SQLException {
        if (initialized) {
            return;
        }
        ensureDatabaseDirectoryExists();

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    account_type TEXT NOT NULL DEFAULT 'REGULAR',
                    age INTEGER NOT NULL DEFAULT 20,
                    gender TEXT NOT NULL DEFAULT 'Not Specified',
                    height_cm REAL NOT NULL DEFAULT 170.0,
                    weight_kg REAL NOT NULL DEFAULT 70.0,
                    created_at TEXT NOT NULL
                );
                """;

        String createWorkoutsTable = """
                CREATE TABLE IF NOT EXISTS workouts (
                    workout_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    workout_type TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    calories_burned REAL NOT NULL,
                    workout_date TEXT NOT NULL,
                    intensity TEXT NOT NULL DEFAULT 'Moderate',
                    steps INTEGER NOT NULL DEFAULT 0,
                    distance_km REAL NOT NULL DEFAULT 0.0,
                    notes TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                );
                """;

        String createGoalsTable = """
                CREATE TABLE IF NOT EXISTS fitness_goals (
                    goal_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL UNIQUE,
                    daily_step_target INTEGER NOT NULL DEFAULT 8000,
                    daily_water_target_ml INTEGER NOT NULL DEFAULT 2500,
                    weekly_workout_target INTEGER NOT NULL DEFAULT 5,
                    daily_calorie_target REAL NOT NULL DEFAULT 500.0,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                );
                """;

        String createWaterTable = """
                CREATE TABLE IF NOT EXISTS water_intake (
                    water_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    amount_ml INTEGER NOT NULL,
                    intake_date TEXT NOT NULL,
                    intake_time TEXT NOT NULL,
                    note TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                );
                """;

        String createActivityRecordsTable = """
                CREATE TABLE IF NOT EXISTS activity_records (
                    record_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    workout_id INTEGER,
                    activity_date TEXT NOT NULL,
                    steps_count INTEGER NOT NULL DEFAULT 0,
                    active_minutes INTEGER NOT NULL DEFAULT 0,
                    calories_burned REAL NOT NULL DEFAULT 0.0,
                    activity_source TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                    FOREIGN KEY (workout_id) REFERENCES workouts(workout_id) ON DELETE CASCADE
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createWorkoutsTable);
            stmt.execute(createGoalsTable);
            stmt.execute(createWaterTable);
            stmt.execute(createActivityRecordsTable);
            initialized = true;
        }
    }

    private static void ensureDatabaseDirectoryExists() {
        File dir = new File(DB_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
