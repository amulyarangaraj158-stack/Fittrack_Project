package com.fittrack.server;

import com.fittrack.model.Achievement;
import com.fittrack.model.ActivityRecord;
import com.fittrack.model.FitnessGoal;
import com.fittrack.model.GoalHistoryEntry;
import com.fittrack.model.NotificationItem;
import com.fittrack.model.SleepLog;
import com.fittrack.model.User;
import com.fittrack.model.UserSettings;
import com.fittrack.model.WaterTracker;
import com.fittrack.model.WeightEntry;
import com.fittrack.model.Workout;
import com.fittrack.model.WorkoutPlan;
import com.fittrack.service.FitnessTracker;
import com.fittrack.service.UserService;
import com.fittrack.service.WorkoutService;
import com.fittrack.util.ValidationUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Pure Java HTTP Server built on JDK 17 com.sun.net.httpserver.HttpServer.
 * Provides static asset serving and authenticated REST API endpoints with
 * session TTL validation, user-scoped ownership enforcement, and tier access checks.
 */
public class FitTrackHttpServer {

    private static final long SESSION_TTL_MS = 24L * 60L * 60L * 1000L;

    private record SessionInfo(int userId, long expiresAtMillis) {
    }

    private final int port;
    private final UserService userService;
    private final WorkoutService workoutService;
    private final FitnessTracker fitnessTracker;
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    public FitTrackHttpServer(int port) {
        this.port = port;
        this.userService = new UserService();
        this.workoutService = new WorkoutService();
        this.fitnessTracker = new FitnessTracker();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path == null || "/".equals(path)) {
            path = "/index.html";
        }
        if (path.contains("..")) {
            sendText(exchange, 403, "Forbidden", "text/plain");
            return;
        }

        String resourcePath = "/web" + path;
        try (InputStream is = FitTrackHttpServer.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                sendText(exchange, 404, "Not Found", "text/plain");
                return;
            }
            byte[] bytes = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", getContentType(path));
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        try {
            if ("POST".equals(method) && "/api/auth/register".equals(path)) {
                handleRegister(exchange);
                return;
            }
            if ("POST".equals(method) && "/api/auth/login".equals(path)) {
                handleLogin(exchange);
                return;
            }
            if ("POST".equals(method) && "/api/auth/logout".equals(path)) {
                handleLogout(exchange);
                return;
            }

            User user = authenticate(exchange);
            if (user == null) {
                sendError(exchange, 401, "Unauthorized. Please sign in.");
                return;
            }

            if ("GET".equals(method) && "/api/dashboard".equals(path)) {
                handleDashboard(exchange, user);
            } else if ("GET".equals(method) && "/api/search".equals(path)) {
                handleGlobalSearch(exchange, user);
            } else if ("GET".equals(method) && "/api/workouts".equals(path)) {
                handleGetWorkouts(exchange, user);
            } else if ("GET".equals(method) && path.startsWith("/api/workouts/")) {
                handleGetWorkoutDetail(exchange, user, extractId(path, "/api/workouts/"));
            } else if ("POST".equals(method) && "/api/workouts".equals(path)) {
                handleAddWorkout(exchange, user);
            } else if ("POST".equals(method) && "/api/workouts/preview".equals(path)) {
                handlePreviewCalories(exchange, user);
            } else if ("PUT".equals(method) && path.startsWith("/api/workouts/")) {
                handleUpdateWorkout(exchange, user, extractId(path, "/api/workouts/"));
            } else if ("DELETE".equals(method) && path.startsWith("/api/workouts/")) {
                handleDeleteWorkout(exchange, user, extractId(path, "/api/workouts/"));
            } else if ("GET".equals(method) && "/api/plans".equals(path)) {
                handleGetPlans(exchange, user);
            } else if ("POST".equals(method) && "/api/plans".equals(path)) {
                handleAddPlan(exchange, user);
            } else if ("PUT".equals(method) && path.startsWith("/api/plans/")) {
                handleUpdatePlan(exchange, user, extractId(path, "/api/plans/"));
            } else if ("POST".equals(method) && path.startsWith("/api/plans/") && path.endsWith("/complete")) {
                String middle = path.substring("/api/plans/".length(), path.length() - "/complete".length());
                handleCompletePlan(exchange, user, parsePositiveId(middle));
            } else if ("DELETE".equals(method) && path.startsWith("/api/plans/")) {
                handleDeletePlan(exchange, user, extractId(path, "/api/plans/"));
            } else if ("GET".equals(method) && "/api/goals".equals(path)) {
                handleGetGoals(exchange, user);
            } else if ("PUT".equals(method) && "/api/goals".equals(path)) {
                handleUpdateGoals(exchange, user);
            } else if ("GET".equals(method) && "/api/goals/suggest".equals(path)) {
                sendJson(exchange, 200, fitnessTracker.suggestTargets(user));
            } else if ("GET".equals(method) && "/api/goals/history".equals(path)) {
                handleGetGoalHistory(exchange, user);
            } else if ("GET".equals(method) && "/api/activities".equals(path)) {
                handleGetActivities(exchange, user);
            } else if ("POST".equals(method) && "/api/steps".equals(path)) {
                handleLogSteps(exchange, user);
            } else if ("DELETE".equals(method) && path.startsWith("/api/activities/")) {
                handleDeleteActivity(exchange, user, extractId(path, "/api/activities/"));
            } else if ("GET".equals(method) && "/api/water".equals(path)) {
                handleGetWater(exchange, user);
            } else if ("POST".equals(method) && "/api/water".equals(path)) {
                handleAddWater(exchange, user);
            } else if ("DELETE".equals(method) && path.startsWith("/api/water/")) {
                handleDeleteWater(exchange, user, extractId(path, "/api/water/"));
            } else if ("GET".equals(method) && "/api/sleep".equals(path)) {
                handleGetSleep(exchange, user);
            } else if ("POST".equals(method) && "/api/sleep".equals(path)) {
                handleAddSleep(exchange, user);
            } else if ("DELETE".equals(method) && path.startsWith("/api/sleep/")) {
                handleDeleteSleep(exchange, user, extractId(path, "/api/sleep/"));
            } else if ("GET".equals(method) && "/api/weight".equals(path)) {
                handleGetWeight(exchange, user);
            } else if ("POST".equals(method) && "/api/weight".equals(path)) {
                handleLogWeight(exchange, user);
            } else if ("PUT".equals(method) && path.startsWith("/api/weight/")) {
                handleUpdateWeight(exchange, user, extractId(path, "/api/weight/"));
            } else if ("DELETE".equals(method) && path.startsWith("/api/weight/")) {
                handleDeleteWeight(exchange, user, extractId(path, "/api/weight/"));
            } else if ("GET".equals(method) && "/api/achievements".equals(path)) {
                handleGetAchievements(exchange, user);
            } else if ("GET".equals(method) && "/api/notifications".equals(path)) {
                handleGetNotifications(exchange, user);
            } else if ("POST".equals(method) && "/api/notifications/read-all".equals(path)) {
                fitnessTracker.markAllNotificationsRead(user.getUserId());
                handleGetNotifications(exchange, user);
            } else if ("POST".equals(method) && path.startsWith("/api/notifications/") && path.endsWith("/read")) {
                String middle = path.substring("/api/notifications/".length(), path.length() - "/read".length());
                fitnessTracker.markNotificationRead(parsePositiveId(middle), user.getUserId());
                handleGetNotifications(exchange, user);
            } else if ("DELETE".equals(method) && path.startsWith("/api/notifications/")) {
                fitnessTracker.deleteNotification(extractId(path, "/api/notifications/"), user.getUserId());
                handleGetNotifications(exchange, user);
            } else if ("GET".equals(method) && "/api/reports".equals(path)) {
                handleGetReports(exchange, user);
            } else if ("GET".equals(method) && "/api/profile".equals(path)) {
                sendJson(exchange, 200, userToMap(user));
            } else if ("PUT".equals(method) && "/api/profile".equals(path)) {
                handleUpdateProfile(exchange, user);
            } else if ("PUT".equals(method) && "/api/profile/password".equals(path)) {
                handleChangePassword(exchange, user);
            } else if ("DELETE".equals(method) && "/api/profile".equals(path)) {
                handleDeleteAccount(exchange, user);
            } else if ("GET".equals(method) && "/api/settings".equals(path)) {
                sendJson(exchange, 200, settingsToMap(fitnessTracker.getSettings(user.getUserId())));
            } else if ("PUT".equals(method) && "/api/settings".equals(path)) {
                handleUpdateSettings(exchange, user);
            } else {
                sendError(exchange, 404, "Endpoint not found.");
            }
        } catch (SecurityException ex) {
            sendError(exchange, 403, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (SQLException ex) {
            sendError(exchange, 500, "Database error occurred.");
        } catch (Exception ex) {
            sendError(exchange, 500, "Server error occurred.");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        User user = userService.register(
                JsonHelper.getString(body, "fullName", ""),
                JsonHelper.getString(body, "email", ""),
                JsonHelper.getString(body, "password", ""),
                JsonHelper.getString(body, "accountType", User.ACCOUNT_REGULAR),
                JsonHelper.getOptionalInt(body, "age", 20),
                JsonHelper.getString(body, "gender", "Not Specified"),
                JsonHelper.getOptionalDouble(body, "heightCm", 170.0),
                JsonHelper.getOptionalDouble(body, "weightKg", 70.0),
                JsonHelper.getString(body, "activityPreference", "Balanced")
        );
        String token = createSession(user.getUserId());
        sendJson(exchange, 201, Map.of("token", token, "user", userToMap(user)));
    }

    private void handleLogin(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        User user = userService.login(
                JsonHelper.getString(body, "email", ""),
                JsonHelper.getString(body, "password", "")
        );
        String token = createSession(user.getUserId());
        sendJson(exchange, 200, Map.of("token", token, "user", userToMap(user)));
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = extractToken(exchange);
        if (token != null) {
            activeSessions.remove(token);
        }
        sendJson(exchange, 200, Map.of("message", "Logged out"));
    }

    private record DateWindow(LocalDate start, LocalDate end, String rangeKey) {
    }

    private DateWindow resolveDateWindow(User user, Map<String, String> queryParams) {
        String range = queryParams.getOrDefault("range", "7d").trim().toLowerCase();
        String fromParam = queryParams.getOrDefault("from", "").trim();
        String toParam = queryParams.getOrDefault("to", "").trim();
        LocalDate today = LocalDate.now();

        LocalDate start;
        LocalDate end = today;

        switch (range) {
            case "30d" -> start = today.minusDays(29);
            case "3m" -> {
                if (!user.hasPremiumAnalyticsAccess()) {
                    throw new SecurityException("Upgrade to Premium membership to access 3-month analytics.");
                }
                start = today.minusDays(89);
            }
            case "6m" -> {
                if (!user.hasPremiumAnalyticsAccess()) {
                    throw new SecurityException("Upgrade to Premium membership to access 6-month analytics.");
                }
                start = today.minusDays(179);
            }
            case "custom" -> {
                if (!ValidationUtils.isValidDate(fromParam) || !ValidationUtils.isValidDate(toParam)) {
                    throw new IllegalArgumentException("Custom date range requires valid 'from' and 'to' dates (YYYY-MM-DD).");
                }
                start = LocalDate.parse(fromParam);
                end = LocalDate.parse(toParam);
                if (start.isAfter(end)) {
                    throw new IllegalArgumentException("'From' date cannot be after 'To' date.");
                }
                long days = ChronoUnit.DAYS.between(start, end) + 1;
                if (days > 31 && !user.hasPremiumAnalyticsAccess()) {
                    throw new SecurityException("Custom ranges exceeding 31 days require Premium membership.");
                }
            }
            default -> {
                range = "7d";
                start = today.minusDays(6);
            }
        }
        return new DateWindow(start, end, range);
    }

    private void handleDashboard(HttpExchange exchange, User user) throws IOException, SQLException {
        int userId = user.getUserId();
        Map<String, String> qp = parseQuery(exchange.getRequestURI().getRawQuery());
        DateWindow window = resolveDateWindow(user, qp);

        long spanDays = ChronoUnit.DAYS.between(window.start(), window.end()) + 1;
        LocalDate prevEnd = window.start().minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(spanDays - 1);

        FitnessGoal goal = fitnessTracker.getOrCreateGoals(userId);
        int todaySteps = workoutService.getTodaySteps(userId);
        double todayCalories = workoutService.getTodayCalories(userId);
        int todayDuration = workoutService.getTodayDuration(userId);
        int todayWater = fitnessTracker.getTodayWaterMl(userId);
        int weeklyWorkouts = workoutService.getWeeklyWorkoutCount(userId);

        Map<String, Object> curPeriod = workoutService.getSummaryBetweenDates(userId, window.start().toString(), window.end().toString());
        Map<String, Object> prevPeriod = workoutService.getSummaryBetweenDates(userId, prevStart.toString(), prevEnd.toString());
        int curSteps = workoutService.getStepsBetweenDates(userId, window.start().toString(), window.end().toString());
        int prevSteps = workoutService.getStepsBetweenDates(userId, prevStart.toString(), prevEnd.toString());

        Map<String, Object> sleepInsights = fitnessTracker.getSleepInsights(userId);
        Map<String, Integer> streaks = fitnessTracker.calculateStreaks(userId);
        ArrayList<Achievement> achievements = fitnessTracker.evaluateAndGetAchievements(userId);
        fitnessTracker.checkGoalAndReminderNotifications(userId);

        // Activity series for charts
        Map<String, Double> calMap = workoutService.getDailyCaloriesBetween(userId, window.start(), window.end());
        Map<String, Integer> durMap = workoutService.getDailyDurationBetween(userId, window.start(), window.end());
        Map<String, Integer> freqMap = workoutService.getDailySessionCountBetween(userId, window.start(), window.end());
        Map<String, Integer> stepsMap = workoutService.getDailyStepsSeries(userId, window.start(), window.end());

        List<Map<String, Object>> activitySeries = new ArrayList<>();
        for (String date : calMap.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date);
            row.put("calories", calMap.get(date));
            row.put("duration", durMap.getOrDefault(date, 0));
            row.put("sessions", freqMap.getOrDefault(date, 0));
            row.put("steps", stepsMap.getOrDefault(date, 0));
            activitySeries.add(row);
        }

        // Breakdown by workout type in selected window
        ArrayList<Workout> windowWorkouts = workoutService.getWorkoutsBetweenDates(userId, window.start().toString(), window.end().toString());
        Map<String, Double> calByType = new LinkedHashMap<>();
        Map<String, Integer> countByType = new LinkedHashMap<>();
        int weeklyMinutes = 0;
        double weeklyDistance = 0.0;
        for (Workout w : windowWorkouts) {
            calByType.merge(w.getWorkoutType(), w.getCaloriesBurned(), Double::sum);
            countByType.merge(w.getWorkoutType(), 1, Integer::sum);
        }
        for (Workout w : workoutService.getWorkoutsLast7Days(userId)) {
            weeklyMinutes += w.getDurationMinutes();
            weeklyDistance += w.getDistanceKm();
        }
        List<Map<String, Object>> typeBreakdown = new ArrayList<>();
        for (Map.Entry<String, Double> e : calByType.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("workoutType", e.getKey());
            item.put("calories", Math.round(e.getValue() * 10.0) / 10.0);
            item.put("sessions", countByType.getOrDefault(e.getKey(), 0));
            typeBreakdown.add(item);
        }

        ArrayList<Workout> all = workoutService.getAllWorkouts(userId);
        List<Map<String, Object>> recent = new ArrayList<>();
        for (int i = 0; i < Math.min(6, all.size()); i++) {
            recent.add(workoutToMap(all.get(i)));
        }

        List<Map<String, Object>> plans = new ArrayList<>();
        for (WorkoutPlan p : fitnessTracker.getPlans(userId)) {
            plans.add(planToMap(p));
        }

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("sessions", curPeriod.get("sessions"));
        comparison.put("prevSessions", prevPeriod.get("sessions"));
        comparison.put("minutes", curPeriod.get("minutes"));
        comparison.put("prevMinutes", prevPeriod.get("minutes"));
        comparison.put("calories", curPeriod.get("calories"));
        comparison.put("prevCalories", prevPeriod.get("calories"));
        comparison.put("steps", curSteps);
        comparison.put("prevSteps", prevSteps);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user", userToMap(user));
        payload.put("settings", settingsToMap(fitnessTracker.getSettings(userId)));
        payload.put("todayDate", LocalDate.now().toString());
        payload.put("range", window.rangeKey());
        payload.put("fromDate", window.start().toString());
        payload.put("toDate", window.end().toString());
        payload.put("todaySteps", todaySteps);
        payload.put("todayCalories", Math.round(todayCalories * 10.0) / 10.0);
        payload.put("todayDuration", todayDuration);
        payload.put("todayWaterMl", todayWater);
        payload.put("weeklyWorkouts", weeklyWorkouts);
        payload.put("weeklyMinutes", weeklyMinutes);
        payload.put("weeklyDistanceKm", Math.round(weeklyDistance * 10.0) / 10.0);
        payload.put("weeklySteps", workoutService.getWeeklySteps(userId));
        payload.put("weeklyAvgSleep", sleepInsights.get("weeklyAvgHours"));
        payload.put("periodStats", comparison);
        payload.put("streaks", streaks);
        payload.put("goals", goalToMap(goal, todaySteps, todayWater, weeklyWorkouts, todayCalories, weeklyMinutes, weeklyDistance));
        payload.put("activitySeries", activitySeries);
        payload.put("typeBreakdown", typeBreakdown);
        payload.put("recentWorkouts", recent);
        payload.put("plans", plans);
        payload.put("recommendations", fitnessTracker.generateRecommendations(user));
        payload.put("achievements", achievementsToList(achievements));
        payload.put("unreadNotifications", fitnessTracker.countUnreadNotifications(userId));

        sendJson(exchange, 200, payload);
    }

    private void handleGlobalSearch(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> qp = parseQuery(exchange.getRequestURI().getRawQuery());
        String q = qp.getOrDefault("q", "").trim();
        if (q.isEmpty()) {
            sendJson(exchange, 200, Map.of("workouts", List.of(), "plans", List.of()));
            return;
        }
        String lower = q.toLowerCase();
        List<Map<String, Object>> workoutMatches = new ArrayList<>();
        for (Workout w : workoutService.getFilteredSortedWorkouts(user.getUserId(), "", "", "", "", q, "date", "desc")) {
            workoutMatches.add(workoutToMap(w));
            if (workoutMatches.size() >= 6) break;
        }
        List<Map<String, Object>> planMatches = new ArrayList<>();
        for (WorkoutPlan p : fitnessTracker.getPlans(user.getUserId())) {
            if (p.getWorkoutName().toLowerCase().contains(lower)
                    || p.getWorkoutType().toLowerCase().contains(lower)
                    || p.getDayOfWeek().toLowerCase().contains(lower)
                    || (p.getNotes() != null && p.getNotes().toLowerCase().contains(lower))) {
                planMatches.add(planToMap(p));
                if (planMatches.size() >= 4) break;
            }
        }
        sendJson(exchange, 200, Map.of("workouts", workoutMatches, "plans", planMatches));
    }

    private void handleGetWorkouts(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> qp = parseQuery(exchange.getRequestURI().getRawQuery());
        ArrayList<Workout> list = workoutService.getFilteredSortedWorkouts(
                user.getUserId(),
                qp.getOrDefault("from", ""),
                qp.getOrDefault("to", ""),
                qp.getOrDefault("type", ""),
                qp.getOrDefault("intensity", ""),
                qp.getOrDefault("q", ""),
                qp.getOrDefault("sortBy", "date"),
                qp.getOrDefault("sortDir", "desc")
        );
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Workout w : list) {
            rows.add(workoutToMap(w));
        }
        sendJson(exchange, 200, Map.of("workouts", rows));
    }

    private void handleGetWorkoutDetail(HttpExchange exchange, User user, int workoutId) throws IOException, SQLException {
        Workout w = workoutService.getWorkoutById(workoutId, user.getUserId());
        if (w == null) {
            sendError(exchange, 404, "Workout not found.");
            return;
        }
        sendJson(exchange, 200, workoutToMap(w));
    }

    private void handlePreviewCalories(HttpExchange exchange, User user) throws IOException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        double calories = workoutService.previewCalories(
                JsonHelper.getString(body, "workoutType", "Walking"),
                JsonHelper.getInt(body, "durationMinutes", "Duration"),
                JsonHelper.getString(body, "intensity", "Moderate"),
                JsonHelper.getOptionalInt(body, "steps", 0),
                JsonHelper.getOptionalDouble(body, "distanceKm", 0.0),
                user.getWeightKg()
        );
        sendJson(exchange, 200, Map.of("caloriesBurned", calories));
    }

    private void handleAddWorkout(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        String type = JsonHelper.getString(body, "workoutType", "");
        String name = JsonHelper.getString(body, "workoutName", type);
        Workout created = workoutService.addWorkout(
                user,
                name,
                type,
                JsonHelper.getInt(body, "durationMinutes", "Duration"),
                JsonHelper.getString(body, "workoutDate", LocalDate.now().toString()),
                JsonHelper.getString(body, "intensity", "Moderate"),
                JsonHelper.getOptionalInt(body, "steps", 0),
                JsonHelper.getOptionalDouble(body, "distanceKm", 0.0),
                JsonHelper.getString(body, "notes", "")
        );
        fitnessTracker.evaluateAndGetAchievements(user.getUserId());
        fitnessTracker.checkGoalAndReminderNotifications(user.getUserId());
        sendJson(exchange, 201, workoutToMap(created));
    }

    private void handleUpdateWorkout(HttpExchange exchange, User user, int workoutId) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        String type = JsonHelper.getString(body, "workoutType", "");
        String name = JsonHelper.getString(body, "workoutName", type);
        boolean updated = workoutService.updateWorkout(
                user, workoutId,
                name,
                type,
                JsonHelper.getInt(body, "durationMinutes", "Duration"),
                JsonHelper.getString(body, "workoutDate", LocalDate.now().toString()),
                JsonHelper.getString(body, "intensity", "Moderate"),
                JsonHelper.getOptionalInt(body, "steps", 0),
                JsonHelper.getOptionalDouble(body, "distanceKm", 0.0),
                JsonHelper.getString(body, "notes", "")
        );
        if (!updated) {
            sendError(exchange, 404, "Workout not found.");
            return;
        }
        fitnessTracker.evaluateAndGetAchievements(user.getUserId());
        sendJson(exchange, 200, Map.of("message", "Workout updated"));
    }

    private void handleDeleteWorkout(HttpExchange exchange, User user, int workoutId) throws IOException, SQLException {
        if (!workoutService.deleteWorkout(workoutId, user.getUserId())) {
            sendError(exchange, 404, "Workout not found.");
            return;
        }
        sendJson(exchange, 200, Map.of("message", "Workout deleted"));
    }

    private void handleGetPlans(HttpExchange exchange, User user) throws IOException, SQLException {
        List<Map<String, Object>> plans = new ArrayList<>();
        for (WorkoutPlan p : fitnessTracker.getPlans(user.getUserId())) {
            plans.add(planToMap(p));
        }
        sendJson(exchange, 200, Map.of(
                "plans", plans,
                "maxAllowed", user.getMaxWorkoutPlansAllowed()
        ));
    }

    private void handleAddPlan(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        String type = JsonHelper.getString(body, "workoutType", "Walking");
        String name = JsonHelper.getString(body, "workoutName", type);
        fitnessTracker.addPlan(
                user,
                JsonHelper.getString(body, "dayOfWeek", "Monday"),
                name,
                type,
                JsonHelper.getInt(body, "plannedMinutes", "Planned Minutes"),
                JsonHelper.getString(body, "intensity", "Moderate"),
                JsonHelper.getString(body, "notes", "")
        );
        handleGetPlans(exchange, user);
    }

    private void handleUpdatePlan(HttpExchange exchange, User user, int planId) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        String type = JsonHelper.getString(body, "workoutType", "Walking");
        String name = JsonHelper.getString(body, "workoutName", type);
        boolean ok = fitnessTracker.updatePlan(
                user.getUserId(),
                planId,
                JsonHelper.getString(body, "dayOfWeek", "Monday"),
                name,
                type,
                JsonHelper.getInt(body, "plannedMinutes", "Planned Minutes"),
                JsonHelper.getString(body, "intensity", "Moderate"),
                JsonHelper.getString(body, "notes", "")
        );
        if (!ok) {
            sendError(exchange, 404, "Workout plan not found.");
            return;
        }
        handleGetPlans(exchange, user);
    }

    private void handleCompletePlan(HttpExchange exchange, User user, int planId) throws IOException, SQLException {
        Workout completed = fitnessTracker.completePlan(user, planId);
        fitnessTracker.evaluateAndGetAchievements(user.getUserId());
        fitnessTracker.checkGoalAndReminderNotifications(user.getUserId());
        sendJson(exchange, 200, Map.of("workout", workoutToMap(completed)));
    }

    private void handleDeletePlan(HttpExchange exchange, User user, int planId) throws IOException, SQLException {
        if (!fitnessTracker.deletePlan(planId, user.getUserId())) {
            sendError(exchange, 404, "Plan not found.");
            return;
        }
        handleGetPlans(exchange, user);
    }

    private void handleGetGoals(HttpExchange exchange, User user) throws IOException, SQLException {
        int userId = user.getUserId();
        FitnessGoal goal = fitnessTracker.getOrCreateGoals(userId);
        int weeklyMinutes = 0;
        double weeklyDistance = 0.0;
        for (Workout w : workoutService.getWorkoutsLast7Days(userId)) {
            weeklyMinutes += w.getDurationMinutes();
            weeklyDistance += w.getDistanceKm();
        }
        Map<String, Object> resp = goalToMap(
                goal,
                workoutService.getTodaySteps(userId),
                fitnessTracker.getTodayWaterMl(userId),
                workoutService.getWeeklyWorkoutCount(userId),
                workoutService.getTodayCalories(userId),
                weeklyMinutes,
                weeklyDistance
        );
        resp.put("history", goalHistoryList(fitnessTracker.getGoalHistory(userId)));
        sendJson(exchange, 200, resp);
    }

    private void handleUpdateGoals(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        fitnessTracker.updateGoals(
                user.getUserId(),
                JsonHelper.getInt(body, "dailyStepTarget", "Daily Step Goal"),
                JsonHelper.getInt(body, "dailyWaterTargetMl", "Daily Water Goal"),
                JsonHelper.getInt(body, "weeklyWorkoutTarget", "Weekly Workout Goal"),
                JsonHelper.getDouble(body, "dailyCalorieTarget", "Daily Calorie Goal"),
                JsonHelper.getOptionalInt(body, "activeMinutesTarget", 150),
                JsonHelper.getOptionalDouble(body, "distanceKmTarget", 20.0)
        );
        handleGetGoals(exchange, user);
    }

    private void handleGetGoalHistory(HttpExchange exchange, User user) throws IOException, SQLException {
        sendJson(exchange, 200, Map.of("history", goalHistoryList(fitnessTracker.getGoalHistory(user.getUserId()))));
    }

    private void handleGetActivities(HttpExchange exchange, User user) throws IOException, SQLException {
        int userId = user.getUserId();
        LocalDate today = LocalDate.now();
        Map<String, Integer> stepSeries = workoutService.getDailyStepsSeries(userId, today.minusDays(6), today);
        List<Map<String, Object>> seriesList = new ArrayList<>();
        for (Map.Entry<String, Integer> e : stepSeries.entrySet()) {
            seriesList.add(Map.of("date", e.getKey(), "steps", e.getValue()));
        }
        List<Map<String, Object>> records = new ArrayList<>();
        for (ActivityRecord r : workoutService.getRecentActivityRecords(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", r.getRecordId());
            m.put("workoutId", r.getWorkoutId());
            m.put("activityDate", r.getActivityDate());
            m.put("stepsCount", r.getStepsCount());
            m.put("activeMinutes", r.getActiveMinutes());
            m.put("caloriesBurned", r.getCaloriesBurned());
            m.put("activitySource", r.getActivitySource());
            records.add(m);
        }
        FitnessGoal goal = fitnessTracker.getOrCreateGoals(userId);
        int todaySteps = workoutService.getTodaySteps(userId);
        sendJson(exchange, 200, Map.of(
                "todaySteps", todaySteps,
                "dailyStepTarget", goal.getDailyStepTarget(),
                "weeklySteps", workoutService.getWeeklySteps(userId),
                "stepSeries", seriesList,
                "records", records
        ));
    }

    private void handleLogSteps(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        int steps = JsonHelper.getInt(body, "steps", "Steps");
        String date = JsonHelper.getString(body, "date", LocalDate.now().toString());
        String source = JsonHelper.getString(body, "activitySource", "Daily Steps");
        workoutService.logDailySteps(user.getUserId(), steps, date, source);
        fitnessTracker.evaluateAndGetAchievements(user.getUserId());
        fitnessTracker.checkGoalAndReminderNotifications(user.getUserId());
        sendJson(exchange, 201, Map.of("message", "Activity logged", "steps", steps));
    }

    private void handleDeleteActivity(HttpExchange exchange, User user, int recordId) throws IOException, SQLException {
        if (!workoutService.deleteManualActivityRecord(recordId, user.getUserId())) {
            sendError(exchange, 400, "Only manual step logs can be deleted here (delete workout entries from My Workouts).");
            return;
        }
        handleGetActivities(exchange, user);
    }

    private void handleGetWater(HttpExchange exchange, User user) throws IOException, SQLException {
        int userId = user.getUserId();
        FitnessGoal goal = fitnessTracker.getOrCreateGoals(userId);
        int todayMl = fitnessTracker.getTodayWaterMl(userId);
        int targetMl = goal.getDailyWaterTargetMl();

        LocalDate today = LocalDate.now();
        Map<String, Integer> weeklyMap = fitnessTracker.getWaterSeries(userId, today.minusDays(6), today);
        List<Map<String, Object>> weeklySeries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : weeklyMap.entrySet()) {
            weeklySeries.add(Map.of("date", e.getKey(), "ml", e.getValue()));
        }

        List<Map<String, Object>> entries = new ArrayList<>();
        for (WaterTracker w : fitnessTracker.getRecentWaterEntries(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("waterId", w.getWaterId());
            m.put("amountMl", w.getAmountMl());
            m.put("intakeDate", w.getIntakeDate());
            m.put("intakeTime", w.getIntakeTime());
            m.put("note", w.getNote());
            entries.add(m);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("todayMl", todayMl);
        resp.put("targetMl", targetMl);
        resp.put("remainingMl", WaterTracker.calculateRemainingMl(todayMl, targetMl));
        resp.put("percent", WaterTracker.calculateHydrationPercent(todayMl, targetMl));
        resp.put("weeklySeries", weeklySeries);
        resp.put("entries", entries);
        sendJson(exchange, 200, resp);
    }

    private void handleAddWater(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        fitnessTracker.logWater(
                user.getUserId(),
                JsonHelper.getInt(body, "amountMl", "Water Amount"),
                JsonHelper.getString(body, "intakeDate", LocalDate.now().toString()),
                JsonHelper.getString(body, "note", "Hydration log")
        );
        fitnessTracker.evaluateAndGetAchievements(user.getUserId());
        handleGetWater(exchange, user);
    }

    private void handleDeleteWater(HttpExchange exchange, User user, int waterId) throws IOException, SQLException {
        if (!fitnessTracker.deleteWaterEntry(waterId, user.getUserId())) {
            sendError(exchange, 404, "Water entry not found.");
            return;
        }
        handleGetWater(exchange, user);
    }

    private void handleGetSleep(HttpExchange exchange, User user) throws IOException, SQLException {
        int userId = user.getUserId();
        LocalDate today = LocalDate.now();
        Map<String, Double> sleepMap = fitnessTracker.getSleepSeries(userId, today.minusDays(6), today);
        List<Map<String, Object>> weeklySeries = new ArrayList<>();
        for (Map.Entry<String, Double> e : sleepMap.entrySet()) {
            weeklySeries.add(Map.of("date", e.getKey(), "hours", e.getValue()));
        }

        List<Map<String, Object>> logs = new ArrayList<>();
        for (SleepLog s : fitnessTracker.getRecentSleepLogs(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sleepId", s.getSleepId());
            m.put("sleepDate", s.getSleepDate());
            m.put("hoursSlept", s.getHoursSlept());
            m.put("qualityRating", s.getQualityRating());
            m.put("notes", s.getNotes());
            logs.add(m);
        }

        Map<String, Object> insights = fitnessTracker.getSleepInsights(userId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("weeklyAvgHours", insights.get("weeklyAvgHours"));
        resp.put("weeklyAvgQuality", insights.get("weeklyAvgQuality"));
        resp.put("insights", insights);
        resp.put("weeklySeries", weeklySeries);
        resp.put("logs", logs);
        sendJson(exchange, 200, resp);
    }

    private void handleAddSleep(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        fitnessTracker.addSleepLog(
                user.getUserId(),
                JsonHelper.getString(body, "sleepDate", LocalDate.now().toString()),
                JsonHelper.getDouble(body, "hoursSlept", "Hours Slept"),
                JsonHelper.getInt(body, "qualityRating", "Quality Rating"),
                JsonHelper.getString(body, "notes", "")
        );
        fitnessTracker.evaluateAndGetAchievements(user.getUserId());
        handleGetSleep(exchange, user);
    }

    private void handleDeleteSleep(HttpExchange exchange, User user, int sleepId) throws IOException, SQLException {
        if (!fitnessTracker.deleteSleepLog(sleepId, user.getUserId())) {
            sendError(exchange, 404, "Sleep log not found.");
            return;
        }
        handleGetSleep(exchange, user);
    }

    private void handleGetWeight(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> qp = parseQuery(exchange.getRequestURI().getRawQuery());
        String from = qp.getOrDefault("from", "");
        String to = qp.getOrDefault("to", "");
        List<Map<String, Object>> list = new ArrayList<>();
        for (WeightEntry w : fitnessTracker.getWeightHistoryFiltered(user.getUserId(), from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("weightId", w.getWeightId());
            m.put("weightKg", w.getWeightKg());
            m.put("recordedDate", w.getRecordedDate());
            list.add(m);
        }
        sendJson(exchange, 200, Map.of("currentWeightKg", user.getWeightKg(), "weightHistory", list));
    }

    private void handleLogWeight(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        double weightKg = JsonHelper.getDouble(body, "weightKg", "Weight");
        String date = JsonHelper.getString(body, "recordedDate", LocalDate.now().toString());
        fitnessTracker.logWeightEntry(user, weightKg, date);
        User refreshed = userService.getUserById(user.getUserId());
        sendJson(exchange, 201, Map.of("user", userToMap(refreshed), "weightHistory", weightList(user.getUserId())));
    }

    private void handleUpdateWeight(HttpExchange exchange, User user, int weightId) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        double weightKg = JsonHelper.getDouble(body, "weightKg", "Weight");
        String date = JsonHelper.getString(body, "recordedDate", LocalDate.now().toString());
        if (!fitnessTracker.updateWeightEntry(user.getUserId(), weightId, weightKg, date)) {
            sendError(exchange, 404, "Weight entry not found.");
            return;
        }
        sendJson(exchange, 200, Map.of("weightHistory", weightList(user.getUserId())));
    }

    private void handleDeleteWeight(HttpExchange exchange, User user, int weightId) throws IOException, SQLException {
        if (!fitnessTracker.deleteWeightEntry(weightId, user.getUserId())) {
            sendError(exchange, 404, "Weight entry not found.");
            return;
        }
        sendJson(exchange, 200, Map.of("weightHistory", weightList(user.getUserId())));
    }

    private void handleGetAchievements(HttpExchange exchange, User user) throws IOException, SQLException {
        ArrayList<Achievement> catalog = fitnessTracker.evaluateAndGetAchievements(user.getUserId());
        Map<String, Integer> streaks = fitnessTracker.calculateStreaks(user.getUserId());
        int unlockedCount = 0;
        for (Achievement a : catalog) {
            if (a.isUnlocked()) unlockedCount++;
        }
        sendJson(exchange, 200, Map.of(
                "streaks", streaks,
                "unlockedCount", unlockedCount,
                "totalCount", catalog.size(),
                "achievements", achievementsToList(catalog)
        ));
    }

    private void handleGetNotifications(HttpExchange exchange, User user) throws IOException, SQLException {
        fitnessTracker.checkGoalAndReminderNotifications(user.getUserId());
        List<Map<String, Object>> items = new ArrayList<>();
        for (NotificationItem n : fitnessTracker.getNotifications(user.getUserId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("notificationId", n.getNotificationId());
            m.put("category", n.getCategory());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("read", n.isRead());
            m.put("createdAt", n.getCreatedAt());
            items.add(m);
        }
        sendJson(exchange, 200, Map.of(
                "unreadCount", fitnessTracker.countUnreadNotifications(user.getUserId()),
                "notifications", items
        ));
    }

    private void handleGetReports(HttpExchange exchange, User user) throws IOException, SQLException {
        int userId = user.getUserId();
        Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
        String rangeParam = queryParams.getOrDefault("range", "").trim().toLowerCase();
        String from = queryParams.getOrDefault("from", "").trim();
        String to = queryParams.getOrDefault("to", "").trim();
        String q = queryParams.getOrDefault("q", "").trim();

        LocalDate today = LocalDate.now();
        LocalDate chartStart = today.minusDays(6);
        LocalDate chartEnd = today;

        if ("30d".equals(rangeParam)) {
            chartStart = today.minusDays(29);
            if (from.isEmpty()) from = chartStart.toString();
            if (to.isEmpty()) to = chartEnd.toString();
        } else if ("3m".equals(rangeParam)) {
            if (!user.hasPremiumAnalyticsAccess()) {
                throw new SecurityException("Upgrade to Premium membership to access 3-month detailed reports.");
            }
            chartStart = today.minusDays(89);
            if (from.isEmpty()) from = chartStart.toString();
            if (to.isEmpty()) to = chartEnd.toString();
        } else if ("6m".equals(rangeParam)) {
            if (!user.hasPremiumAnalyticsAccess()) {
                throw new SecurityException("Upgrade to Premium membership to access 6-month detailed reports.");
            }
            chartStart = today.minusDays(179);
            if (from.isEmpty()) from = chartStart.toString();
            if (to.isEmpty()) to = chartEnd.toString();
        } else if (!from.isEmpty() && !to.isEmpty()) {
            if (!ValidationUtils.isValidDate(from) || !ValidationUtils.isValidDate(to)) {
                throw new IllegalArgumentException("Invalid report date range.");
            }
            LocalDate s = LocalDate.parse(from);
            LocalDate e = LocalDate.parse(to);
            if (s.isAfter(e)) {
                throw new IllegalArgumentException("'From' date cannot be after 'To' date.");
            }
            long days = ChronoUnit.DAYS.between(s, e) + 1;
            if (days > 31 && !user.hasPremiumAnalyticsAccess()) {
                throw new SecurityException("Date ranges exceeding 31 days require Premium membership.");
            }
            chartStart = s;
            chartEnd = e;
        } else if ("7d".equals(rangeParam)) {
            if (from.isEmpty()) from = chartStart.toString();
            if (to.isEmpty()) to = chartEnd.toString();
        }

        ArrayList<Workout> filtered = workoutService.getFilteredWorkouts(userId, from, to, q);
        int totalMinutes = 0;
        double totalCalories = 0.0;
        double totalDistance = 0.0;
        Map<String, Double> caloriesByType = new LinkedHashMap<>();
        Map<String, Integer> countByType = new LinkedHashMap<>();

        List<Map<String, Object>> history = new ArrayList<>();
        for (Workout w : filtered) {
            totalMinutes += w.getDurationMinutes();
            totalCalories += w.getCaloriesBurned();
            totalDistance += w.getDistanceKm();
            caloriesByType.merge(w.getWorkoutType(), w.getCaloriesBurned(), Double::sum);
            countByType.merge(w.getWorkoutType(), 1, Integer::sum);
            history.add(workoutToMap(w));
        }

        Map<String, Double> calSeries = workoutService.getDailyCaloriesBetween(userId, chartStart, chartEnd);
        Map<String, Integer> durSeries = workoutService.getDailyDurationBetween(userId, chartStart, chartEnd);
        Map<String, Integer> waterSeriesMap = fitnessTracker.getWaterSeries(userId, today.minusDays(6), today);
        Map<String, Double> sleepSeriesMap = fitnessTracker.getSleepSeries(userId, today.minusDays(6), today);

        List<Map<String, Object>> dailySeries = new ArrayList<>();
        for (String date : calSeries.keySet()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("date", date);
            d.put("calories", calSeries.get(date));
            d.put("duration", durSeries.getOrDefault(date, 0));
            dailySeries.add(d);
        }

        List<Map<String, Object>> waterSeries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : waterSeriesMap.entrySet()) {
            waterSeries.add(Map.of("date", e.getKey(), "ml", e.getValue()));
        }

        List<Map<String, Object>> sleepSeries = new ArrayList<>();
        for (Map.Entry<String, Double> e : sleepSeriesMap.entrySet()) {
            sleepSeries.add(Map.of("date", e.getKey(), "hours", e.getValue()));
        }

        List<Map<String, Object>> typeBreakdown = new ArrayList<>();
        for (Map.Entry<String, Double> e : caloriesByType.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("workoutType", e.getKey());
            item.put("calories", Math.round(e.getValue() * 10.0) / 10.0);
            item.put("sessions", countByType.getOrDefault(e.getKey(), 0));
            typeBreakdown.add(item);
        }

        FitnessGoal goal = fitnessTracker.getOrCreateGoals(userId);
        int todaySteps = workoutService.getTodaySteps(userId);
        int todayWater = fitnessTracker.getTodayWaterMl(userId);
        int weeklyWorkouts = workoutService.getWeeklyWorkoutCount(userId);
        double todayCalories = workoutService.getTodayCalories(userId);
        int weeklyMinutes = 0;
        double weeklyDist = 0.0;
        for (Workout w : workoutService.getWorkoutsLast7Days(userId)) {
            weeklyMinutes += w.getDurationMinutes();
            weeklyDist += w.getDistanceKm();
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("premiumUnlocked", user.hasPremiumAnalyticsAccess());
        resp.put("totalSessions", filtered.size());
        resp.put("totalMinutes", totalMinutes);
        resp.put("totalCalories", Math.round(totalCalories * 10.0) / 10.0);
        resp.put("totalDistanceKm", Math.round(totalDistance * 10.0) / 10.0);
        resp.put("weeklySteps", workoutService.getWeeklySteps(userId));
        resp.put("dailySeries", dailySeries);
        resp.put("typeBreakdown", typeBreakdown);
        resp.put("history", history);
        resp.put("weightHistory", weightList(userId));
        resp.put("waterSeries", waterSeries);
        resp.put("sleepSeries", sleepSeries);
        resp.put("sleepInsights", fitnessTracker.getSleepInsights(userId));
        resp.put("goalSummary", goalToMap(goal, todaySteps, todayWater, weeklyWorkouts, todayCalories, weeklyMinutes, weeklyDist));
        resp.put("streaks", fitnessTracker.calculateStreaks(userId));
        resp.put("achievements", achievementsToList(fitnessTracker.evaluateAndGetAchievements(userId)));
        sendJson(exchange, 200, resp);
    }

    private void handleUpdateProfile(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        User updated = userService.updateProfile(
                user,
                JsonHelper.getString(body, "fullName", user.getFullName()),
                JsonHelper.getString(body, "accountType", user.getAccountType()),
                JsonHelper.getInt(body, "age", "Age"),
                JsonHelper.getString(body, "gender", user.getGender()),
                JsonHelper.getDouble(body, "heightCm", "Height"),
                JsonHelper.getDouble(body, "weightKg", "Weight"),
                JsonHelper.getString(body, "activityPreference", user.getActivityPreference())
        );
        sendJson(exchange, 200, userToMap(updated));
    }

    private void handleChangePassword(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        userService.changePassword(
                user,
                JsonHelper.getString(body, "currentPassword", ""),
                JsonHelper.getString(body, "newPassword", "")
        );
        sendJson(exchange, 200, Map.of("message", "Password updated successfully."));
    }

    private void handleDeleteAccount(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        String password = JsonHelper.getString(body, "password", "");
        boolean deleted = userService.deleteAccount(user.getUserId(), password);
        if (!deleted) {
            sendError(exchange, 404, "Account could not be deleted.");
            return;
        }
        String token = extractToken(exchange);
        if (token != null) {
            activeSessions.remove(token);
        }
        sendJson(exchange, 200, Map.of("message", "Account deleted."));
    }

    private void handleUpdateSettings(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, String> body = JsonHelper.parseObject(readBody(exchange));
        UserSettings current = fitnessTracker.getSettings(user.getUserId());
        current.setThemeMode(JsonHelper.getString(body, "themeMode", current.getThemeMode()));
        current.setWorkoutReminders(JsonHelper.getBoolean(body, "workoutReminders", current.isWorkoutReminders()));
        current.setWaterReminders(JsonHelper.getBoolean(body, "waterReminders", current.isWaterReminders()));
        current.setGoalAlerts(JsonHelper.getBoolean(body, "goalAlerts", current.isGoalAlerts()));
        current.setAchievementAlerts(JsonHelper.getBoolean(body, "achievementAlerts", current.isAchievementAlerts()));
        current.setWeeklyDigest(JsonHelper.getBoolean(body, "weeklyDigest", current.isWeeklyDigest()));
        UserSettings saved = fitnessTracker.updateSettings(current);
        sendJson(exchange, 200, settingsToMap(saved));
    }

    private List<Map<String, Object>> weightList(int userId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WeightEntry w : fitnessTracker.getWeightHistory(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("weightId", w.getWeightId());
            m.put("weightKg", w.getWeightKg());
            m.put("recordedDate", w.getRecordedDate());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> goalHistoryList(ArrayList<GoalHistoryEntry> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (GoalHistoryEntry g : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("historyId", g.getHistoryId());
            m.put("dailyStepTarget", g.getDailyStepTarget());
            m.put("dailyWaterTargetMl", g.getDailyWaterTargetMl());
            m.put("weeklyWorkoutTarget", g.getWeeklyWorkoutTarget());
            m.put("dailyCalorieTarget", g.getDailyCalorieTarget());
            m.put("activeMinutesTarget", g.getActiveMinutesTarget());
            m.put("distanceKmTarget", g.getDistanceKmTarget());
            m.put("recordedAt", g.getRecordedAt());
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> achievementsToList(ArrayList<Achievement> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Achievement a : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("badgeCode", a.getBadgeCode());
            m.put("title", a.getTitle());
            m.put("description", a.getDescription());
            m.put("unlockedAt", a.getUnlockedAt() == null ? "" : a.getUnlockedAt());
            m.put("unlocked", a.isUnlocked());
            m.put("progressPercent", a.getProgressPercent());
            m.put("currentValue", a.getCurrentValue());
            m.put("targetValue", a.getTargetValue());
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> settingsToMap(UserSettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("themeMode", s.getThemeMode());
        m.put("workoutReminders", s.isWorkoutReminders());
        m.put("waterReminders", s.isWaterReminders());
        m.put("goalAlerts", s.isGoalAlerts());
        m.put("achievementAlerts", s.isAchievementAlerts());
        m.put("weeklyDigest", s.isWeeklyDigest());
        m.put("updatedAt", s.getUpdatedAt() == null ? "" : s.getUpdatedAt());
        return m;
    }

    private Map<String, Object> planToMap(WorkoutPlan p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("planId", p.getPlanId());
        m.put("dayOfWeek", p.getDayOfWeek());
        m.put("workoutName", p.getWorkoutName());
        m.put("workoutType", p.getWorkoutType());
        m.put("plannedMinutes", p.getPlannedMinutes());
        m.put("intensity", p.getIntensity());
        m.put("notes", p.getNotes() == null ? "" : p.getNotes());
        m.put("completedDate", p.getCompletedDate() == null ? "" : p.getCompletedDate());
        return m;
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    private User authenticate(HttpExchange exchange) throws SQLException {
        String token = extractToken(exchange);
        if (token == null || token.isEmpty()) {
            return null;
        }
        SessionInfo session = activeSessions.get(token);
        if (session == null) {
            return null;
        }
        if (System.currentTimeMillis() > session.expiresAtMillis()) {
            activeSessions.remove(token);
            return null;
        }
        return userService.getUserById(session.userId());
    }

    private String createSession(int userId) {
        long now = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(e -> now > e.getValue().expiresAtMillis());
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, new SessionInfo(userId, now + SESSION_TTL_MS));
        return token;
    }

    private String extractToken(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        String auth = headers.getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        String custom = headers.getFirst("X-Auth-Token");
        return custom != null ? custom.trim() : null;
    }

    private int extractId(String path, String prefix) {
        return parsePositiveId(path.substring(prefix.length()).trim());
    }

    private int parsePositiveId(String raw) {
        try {
            int id = Integer.parseInt(raw.trim());
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid record ID.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid record ID.");
        }
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getUserId());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("accountType", u.getAccountType());
        m.put("membershipBadge", u.getMembershipBadge());
        m.put("membershipBenefits", u.getMembershipBenefits());
        m.put("hasPremiumAnalytics", u.hasPremiumAnalyticsAccess());
        m.put("hasExtendedInsights", u.hasExtendedInsightsAccess());
        m.put("maxWorkoutPlans", u.getMaxWorkoutPlansAllowed());
        m.put("age", u.getAge());
        m.put("gender", u.getGender());
        m.put("heightCm", u.getHeightCm());
        m.put("weightKg", u.getWeightKg());
        m.put("activityPreference", u.getActivityPreference());
        m.put("bmi", u.calculateBMI());
        m.put("bmiCategory", u.getBmiCategory());
        m.put("createdAt", u.getCreatedAt() == null ? "" : u.getCreatedAt());
        return m;
    }

    private Map<String, Object> workoutToMap(Workout w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("workoutId", w.getWorkoutId());
        m.put("workoutName", w.getWorkoutName());
        m.put("workoutType", w.getWorkoutType());
        m.put("durationMinutes", w.getDurationMinutes());
        m.put("caloriesBurned", w.getCaloriesBurned());
        m.put("workoutDate", w.getWorkoutDate());
        m.put("intensity", w.getIntensity());
        m.put("steps", w.getSteps());
        m.put("distanceKm", w.getDistanceKm());
        m.put("notes", w.getNotes() == null ? "" : w.getNotes());
        m.put("summary", w.trackActivity());
        return m;
    }

    private Map<String, Object> goalToMap(FitnessGoal g, int todaySteps, int todayWater,
                                            int weeklyWorkouts, double todayCalories,
                                            int weeklyMinutes, double weeklyDistanceKm) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dailyStepTarget", g.getDailyStepTarget());
        m.put("dailyWaterTargetMl", g.getDailyWaterTargetMl());
        m.put("weeklyWorkoutTarget", g.getWeeklyWorkoutTarget());
        m.put("dailyCalorieTarget", g.getDailyCalorieTarget());
        m.put("activeMinutesTarget", g.getActiveMinutesTarget());
        m.put("distanceKmTarget", g.getDistanceKmTarget());
        m.put("todaySteps", todaySteps);
        m.put("todayWaterMl", todayWater);
        m.put("weeklyWorkouts", weeklyWorkouts);
        m.put("todayCalories", Math.round(todayCalories * 10.0) / 10.0);
        m.put("weeklyMinutes", weeklyMinutes);
        m.put("weeklyDistanceKm", Math.round(weeklyDistanceKm * 10.0) / 10.0);
        m.put("stepPercent", g.calculateCompletionPercentage(todaySteps, g.getDailyStepTarget()));
        m.put("waterPercent", g.calculateCompletionPercentage(todayWater, g.getDailyWaterTargetMl()));
        m.put("weeklyPercent", g.calculateCompletionPercentage(weeklyWorkouts, g.getWeeklyWorkoutTarget()));
        m.put("caloriePercent", g.calculateCompletionPercentage(todayCalories, g.getDailyCalorieTarget()));
        m.put("minutesPercent", g.calculateCompletionPercentage(weeklyMinutes, g.getActiveMinutesTarget()));
        m.put("distancePercent", g.calculateCompletionPercentage(weeklyDistanceKm, g.getDistanceKmTarget()));
        return m;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        sendText(exchange, status, JsonHelper.toJson(data), "application/json; charset=UTF-8");
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, Map.of("error", message));
    }

    private void sendText(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
