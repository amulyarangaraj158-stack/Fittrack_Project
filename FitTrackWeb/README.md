# FitTrack – Personal Fitness & Analytics Platform (Java 17 + SQLite + Web UI)

## Architecture
- **Backend**: Java 17 + `com.sun.net.httpserver.HttpServer` + JDBC (`sqlite-jdbc`)
- **OOP Design**:
  - **Encapsulation**: Private domain fields with validated getters/setters across all models (`User`, `Workout`, `FitnessGoal`, `WorkoutPlan`, `SleepLog`, `WeightEntry`, `NotificationItem`, `UserSettings`, `GoalHistoryEntry`).
  - **Abstraction & Inheritance**: `abstract class User` (`RegularUser`, `PremiumUser` with tier enforcement for plan caps and extended analytics) and `abstract class Workout` (`Walking`, `Running`, `Cycling`, `Swimming`, `Yoga`, `StrengthTraining`, `HIIT`, `CustomWorkout`).
  - **Polymorphism & Interfaces**: `Trackable` interface implemented across activity/workout tracking; runtime polymorphism via `Workout.calculateCalories()`.
- **Security**: Salted `PBKDF2WithHmacSHA256` password hashing (`PasswordUtils`, backward-compatible legacy SHA-256 verification with transparent rehash on login), 24h session TTL tokens, prepared SQL statements, user-scoped ownership enforcement, and backend HTTP 403 tier checks.
- **Database**: SQLite (`database/fittrack_web.db`) with 13 tables (`Users`, `Workouts`, `FitnessGoals`, `WaterIntake`, `ActivityRecords`, `UserAchievements`, `WorkoutPlans`, `SleepLogs`, `WeightHistory`, `Notifications`, `UserSettings`, `GoalHistory`, `WorkoutStreaks`) plus idempotent column migrations.
- **Frontend**: 12-module SaaS web workspace (`src/main/resources/web/`) featuring a Dark Navy sidebar, Light/Dark themes, topbar global search/notifications/profile/quick-add, Canvas sparklines & charts, CSV export, and `@media print` PDF support.
- **Tests**: JUnit 5 unit tests (`src/test/java/com/fittrack/`).

## How to Run (JDK 17+ & Maven 3.8+)
```bash
cd FitTrackWeb
mvn test
mvn compile exec:java
```
Then open `http://localhost:8080` in your browser.

## Demo Account
- **Email**: `alex@fittrack.com`
- **Password**: `FitTrack@123`
(Or click **Create new account** on the login page to register as Regular or Premium.)
