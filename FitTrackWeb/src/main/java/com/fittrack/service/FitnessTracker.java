package com.fittrack.service;

import com.fittrack.dao.AchievementDAO;
import com.fittrack.dao.GoalDAO;
import com.fittrack.dao.NotificationDAO;
import com.fittrack.dao.SleepDAO;
import com.fittrack.dao.UserDAO;
import com.fittrack.dao.UserSettingsDAO;
import com.fittrack.dao.WaterDAO;
import com.fittrack.dao.WeightHistoryDAO;
import com.fittrack.dao.WorkoutPlanDAO;
import com.fittrack.dao.WorkoutStreakDAO;
import com.fittrack.model.Achievement;
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
import com.fittrack.util.ValidationUtils;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Central coordinator service aggregating Dashboard metrics, Fitness Goals,
 * Water Tracking, Streaks, Achievements, Planner, Sleep, Weight Trends, Notifications,
 * Settings, and Personalized Recommendations.
 */
public class FitnessTracker {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final GoalDAO goalDAO;
    private final WaterDAO waterDAO;
    private final AchievementDAO achievementDAO;
    private final WorkoutPlanDAO workoutPlanDAO;
    private final SleepDAO sleepDAO;
    private final WeightHistoryDAO weightHistoryDAO;
    private final UserDAO userDAO;
    private final NotificationDAO notificationDAO;
    private final UserSettingsDAO userSettingsDAO;
    private final WorkoutStreakDAO workoutStreakDAO;
    private final WorkoutService workoutService;

    public FitnessTracker() {
        this.goalDAO = new GoalDAO();
        this.waterDAO = new WaterDAO();
        this.achievementDAO = new AchievementDAO();
        this.workoutPlanDAO = new WorkoutPlanDAO();
        this.sleepDAO = new SleepDAO();
        this.weightHistoryDAO = new WeightHistoryDAO();
        this.userDAO = new UserDAO();
        this.notificationDAO = new NotificationDAO();
        this.userSettingsDAO = new UserSettingsDAO();
        this.workoutStreakDAO = new WorkoutStreakDAO();
        this.workoutService = new WorkoutService();
    }

    // --- Goals & History ---
    public FitnessGoal getOrCreateGoals(int userId) throws SQLException {
        FitnessGoal goal = goalDAO.findByUserId(userId);
        if (goal == null) {
            goal = goalDAO.saveOrUpdate(new FitnessGoal(userId, 8000, 2500, 5, 500.0, 150, 20.0));
        }
        return goal;
    }

    public FitnessGoal updateGoals(int userId, int dailyStepTarget, int dailyWaterTargetMl,
                                   int weeklyWorkoutTarget, double dailyCalorieTarget,
                                   int activeMinutesTarget, double distanceKmTarget) throws SQLException {
        FitnessGoal goal = new FitnessGoal(userId, dailyStepTarget, dailyWaterTargetMl,
                weeklyWorkoutTarget, dailyCalorieTarget, activeMinutesTarget, distanceKmTarget);
        return goalDAO.saveOrUpdate(goal);
    }

    public FitnessGoal updateGoals(int userId, int dailyStepTarget, int dailyWaterTargetMl,
                                   int weeklyWorkoutTarget, double dailyCalorieTarget) throws SQLException {
        FitnessGoal current = getOrCreateGoals(userId);
        return updateGoals(userId, dailyStepTarget, dailyWaterTargetMl, weeklyWorkoutTarget,
                dailyCalorieTarget, current.getActiveMinutesTarget(), current.getDistanceKmTarget());
    }

    public ArrayList<GoalHistoryEntry> getGoalHistory(int userId) throws SQLException {
        return goalDAO.findGoalHistory(userId, 20);
    }

    public Map<String, Object> suggestTargets(User user) {
        int waterMl = (int) Math.round((user.getWeightKg() * 35.0) / 50.0) * 50;
        waterMl = Math.max(1500, Math.min(4500, waterMl));

        String pref = user.getActivityPreference() == null ? "Balanced" : user.getActivityPreference();
        int stepTarget = switch (pref) {
            case "Weight Management", "Cardio & Endurance" -> 10000;
            case "Strength & Muscle" -> 7500;
            default -> 8500;
        };

        double calorieTarget = switch (pref) {
            case "Weight Management" -> Math.round(user.getWeightKg() * 8.0);
            case "Cardio & Endurance" -> Math.round(user.getWeightKg() * 7.5);
            case "Mindfulness & Flexibility" -> Math.round(user.getWeightKg() * 5.0);
            default -> Math.round(user.getWeightKg() * 6.5);
        };

        int weeklySessions = "Mindfulness & Flexibility".equals(pref) ? 4 : 5;
        int activeMinutesTarget = switch (pref) {
            case "Cardio & Endurance", "Weight Management" -> 210;
            case "Mindfulness & Flexibility" -> 140;
            default -> 165;
        };
        double distanceKmTarget = switch (pref) {
            case "Cardio & Endurance" -> 28.0;
            case "Weight Management" -> 22.0;
            case "Strength & Muscle" -> 12.0;
            default -> 18.0;
        };

        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("dailyStepTarget", stepTarget);
        suggestion.put("dailyWaterTargetMl", waterMl);
        suggestion.put("weeklyWorkoutTarget", weeklySessions);
        suggestion.put("dailyCalorieTarget", calorieTarget);
        suggestion.put("activeMinutesTarget", activeMinutesTarget);
        suggestion.put("distanceKmTarget", distanceKmTarget);
        return suggestion;
    }

    // --- Water Tracking ---
    public WaterTracker logWater(int userId, int amountMl) throws SQLException {
        return logWater(userId, amountMl, LocalDate.now().toString(), "Hydration log");
    }

    public WaterTracker logWater(int userId, int amountMl, String intakeDate, String note) throws SQLException {
        if (!ValidationUtils.isValidDate(intakeDate)) {
            throw new IllegalArgumentException("Intake date must be in YYYY-MM-DD format.");
        }
        String nowTime = LocalTime.now().format(TIME_FORMAT);
        WaterTracker entry = new WaterTracker(userId, amountMl, intakeDate, nowTime, note);
        WaterTracker saved = waterDAO.addWaterIntake(entry);
        checkGoalAndReminderNotifications(userId);
        return saved;
    }

    public boolean deleteWaterEntry(int waterId, int userId) throws SQLException {
        return waterDAO.deleteWaterIntake(waterId, userId);
    }

    public int getTodayWaterMl(int userId) throws SQLException {
        return waterDAO.getTotalWaterByDate(userId, LocalDate.now().toString());
    }

    public ArrayList<WaterTracker> getRecentWaterEntries(int userId) throws SQLException {
        return waterDAO.findRecentByUser(userId, 30);
    }

    public Map<String, Integer> getWaterSeries(int userId, LocalDate start, LocalDate end) throws SQLException {
        Map<String, Integer> raw = waterDAO.getDailyTotalsBetween(userId, start.toString(), end.toString());
        Map<String, Integer> filled = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = cursor.toString();
            filled.put(key, raw.getOrDefault(key, 0));
            cursor = cursor.plusDays(1);
        }
        return filled;
    }

    // --- Streaks ---
    public Map<String, Integer> calculateStreaks(int userId) throws SQLException {
        ArrayList<String> activeDatesDesc = workoutService.getActiveDates(userId);
        Set<LocalDate> dateSet = new HashSet<>();
        for (String d : activeDatesDesc) {
            try {
                dateSet.add(LocalDate.parse(d));
            } catch (Exception ignored) {
            }
        }

        int currentStreak = 0;
        LocalDate cursor = LocalDate.now();
        if (!dateSet.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        while (dateSet.contains(cursor)) {
            currentStreak++;
            cursor = cursor.minusDays(1);
        }

        int longestStreak = 0;
        for (LocalDate d : dateSet) {
            if (!dateSet.contains(d.minusDays(1))) {
                int run = 0;
                LocalDate walk = d;
                while (dateSet.contains(walk)) {
                    run++;
                    walk = walk.plusDays(1);
                }
                if (run > longestStreak) {
                    longestStreak = run;
                }
            }
        }

        String lastActive = activeDatesDesc.isEmpty() ? "" : activeDatesDesc.get(0);
        workoutStreakDAO.upsertStreak(userId, currentStreak, longestStreak, lastActive);

        Map<String, Integer> res = new LinkedHashMap<>();
        res.put("currentStreak", currentStreak);
        res.put("longestStreak", longestStreak);
        return res;
    }

    // --- Achievements (Database-Verified, Locked + Unlocked + Progress) ---
    public ArrayList<Achievement> evaluateAndGetAchievements(int userId) throws SQLException {
        ArrayList<Workout> allWorkouts = workoutService.getAllWorkouts(userId);
        ArrayList<Workout> weeklyWorkouts = workoutService.getWorkoutsLast7Days(userId);
        FitnessGoal goal = getOrCreateGoals(userId);
        int todaySteps = workoutService.getTodaySteps(userId);
        int todayWater = getTodayWaterMl(userId);
        int distinctTypes = workoutService.countDistinctTypes(userId);
        int sleepCount = sleepDAO.countLogsByUser(userId);
        Map<String, Integer> streaks = calculateStreaks(userId);
        int longestStreak = streaks.getOrDefault("longestStreak", 0);

        double weeklyCalories = 0.0;
        for (Workout w : weeklyWorkouts) {
            weeklyCalories += w.getCaloriesBurned();
        }

        UserSettings settings = getSettings(userId);

        awardIfMet(userId, settings, "FIRST_WORKOUT", "First Workout",
                "Recorded your first workout session in FitTrack.", allWorkouts.size() >= 1);
        awardIfMet(userId, settings, "STREAK_7", "7-Day Consistency",
                "Maintained recorded activity for 7 consecutive days.", longestStreak >= 7);
        awardIfMet(userId, settings, "TEN_WORKOUTS", "10 Workouts Completed",
                "Completed 10 or more workout sessions.", allWorkouts.size() >= 10);
        awardIfMet(userId, settings, "HYDRATION_CHAMPION", "Hydration Champion",
                "Reached 100% of your daily water intake target.", todayWater >= goal.getDailyWaterTargetMl());
        awardIfMet(userId, settings, "WEEKLY_GOAL_ACHIEVED", "Weekly Goal Achieved",
                "Met your weekly workout frequency target.", weeklyWorkouts.size() >= goal.getWeeklyWorkoutTarget());
        awardIfMet(userId, settings, "WORKOUT_EXPLORER", "Workout Explorer",
                "Logged at least 4 distinct workout types.", distinctTypes >= 4);
        awardIfMet(userId, settings, "STEP_10K", "10K Steps Day",
                "Logged 10,000 or more steps in a single day.", todaySteps >= 10000);
        awardIfMet(userId, settings, "CALORIE_2K_WEEK", "2,000+ Weekly Burn",
                "Burned over 2,000 kcal in the last 7 days.", weeklyCalories >= 2000.0);
        awardIfMet(userId, settings, "SLEEP_TRACKER_5", "Sleep Consistency Logger",
                "Recorded at least 5 sleep recovery entries.", sleepCount >= 5);

        Map<String, Achievement> unlockedMap = achievementDAO.findByUserIndexed(userId);
        ArrayList<Achievement> catalog = new ArrayList<>();
        catalog.add(buildCatalogItem(unlockedMap, "FIRST_WORKOUT", "First Workout",
                "Recorded your first workout session in FitTrack.", allWorkouts.size(), 1));
        catalog.add(buildCatalogItem(unlockedMap, "TEN_WORKOUTS", "10 Workouts Completed",
                "Completed 10 or more workout sessions.", allWorkouts.size(), 10));
        catalog.add(buildCatalogItem(unlockedMap, "STREAK_7", "7-Day Consistency",
                "Maintained recorded activity for 7 consecutive days.", longestStreak, 7));
        catalog.add(buildCatalogItem(unlockedMap, "HYDRATION_CHAMPION", "Hydration Champion",
                "Reached 100% of your daily water intake target.", todayWater, goal.getDailyWaterTargetMl()));
        catalog.add(buildCatalogItem(unlockedMap, "WEEKLY_GOAL_ACHIEVED", "Weekly Goal Achieved",
                "Met your weekly workout frequency target.", weeklyWorkouts.size(), goal.getWeeklyWorkoutTarget()));
        catalog.add(buildCatalogItem(unlockedMap, "WORKOUT_EXPLORER", "Workout Explorer",
                "Logged at least 4 distinct workout types.", distinctTypes, 4));
        catalog.add(buildCatalogItem(unlockedMap, "STEP_10K", "10K Steps Day",
                "Logged 10,000 or more steps in a single day.", todaySteps, 10000));
        catalog.add(buildCatalogItem(unlockedMap, "CALORIE_2K_WEEK", "2,000+ Weekly Burn",
                "Burned over 2,000 kcal in the last 7 days.", (int) Math.round(weeklyCalories), 2000));
        catalog.add(buildCatalogItem(unlockedMap, "SLEEP_TRACKER_5", "Sleep Consistency Logger",
                "Recorded at least 5 sleep recovery entries.", sleepCount, 5));

        return catalog;
    }

    private void awardIfMet(int userId, UserSettings settings, String code, String title,
                            String desc, boolean conditionMet) throws SQLException {
        if (!conditionMet) {
            return;
        }
        boolean newlyUnlocked = achievementDAO.unlockIfNotEarned(userId, code, title, desc);
        if (newlyUnlocked && settings.isAchievementAlerts()) {
            notificationDAO.addNotification(userId, "Achievement",
                    "Badge Unlocked: " + title, desc);
        }
    }

    private Achievement buildCatalogItem(Map<String, Achievement> unlockedMap, String code,
                                         String title, String desc, int currentVal, int targetVal) {
        Achievement existing = unlockedMap.get(code);
        Achievement item = new Achievement();
        item.setBadgeCode(code);
        item.setTitle(title);
        item.setDescription(desc);
        item.setTargetValue(targetVal);
        if (existing != null) {
            item.setAchievementId(existing.getAchievementId());
            item.setUserId(existing.getUserId());
            item.setUnlockedAt(existing.getUnlockedAt());
            item.setUnlocked(true);
            item.setCurrentValue(Math.max(targetVal, currentVal));
            item.setProgressPercent(100);
        } else {
            item.setUnlockedAt("");
            item.setUnlocked(false);
            item.setCurrentValue(Math.min(targetVal, currentVal));
            int pct = targetVal <= 0 ? 0 : (int) Math.min(99, Math.round((currentVal * 100.0) / targetVal));
            item.setProgressPercent(pct);
        }
        return item;
    }

    // --- Notifications ---
    public void checkGoalAndReminderNotifications(int userId) throws SQLException {
        UserSettings settings = getSettings(userId);
        String today = LocalDate.now().toString();
        FitnessGoal goal = getOrCreateGoals(userId);
        int todaySteps = workoutService.getTodaySteps(userId);
        int todayWater = getTodayWaterMl(userId);
        int weeklyWorkouts = workoutService.getWeeklyWorkoutCount(userId);

        if (settings.isGoalAlerts()) {
            if (todaySteps >= goal.getDailyStepTarget()) {
                createDailyNotificationOnce(userId, "Goal", "Daily Step Goal Reached",
                        "You reached " + todaySteps + " steps today.", today);
            }
            if (todayWater >= goal.getDailyWaterTargetMl()) {
                createDailyNotificationOnce(userId, "Goal", "Daily Hydration Target Reached",
                        "You logged " + todayWater + " ml of water today.", today);
            }
            if (weeklyWorkouts >= goal.getWeeklyWorkoutTarget()) {
                createDailyNotificationOnce(userId, "Goal", "Weekly Workout Target Completed",
                        "You completed " + weeklyWorkouts + " workouts this week.", today);
            }
        }

        if (settings.isWorkoutReminders()) {
            String todayName = DayOfWeek.from(LocalDate.now()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            for (WorkoutPlan p : getPlans(userId)) {
                if (todayName.equalsIgnoreCase(p.getDayOfWeek()) && (p.getCompletedDate() == null || p.getCompletedDate().isBlank())) {
                    createDailyNotificationOnce(userId, "Reminder",
                            "Scheduled Workout Today: " + p.getWorkoutName(),
                            p.getPlannedMinutes() + " min (" + p.getIntensity() + " intensity) planned for " + p.getDayOfWeek() + ".",
                            today);
                }
            }
        }

        if (settings.isWaterReminders() && todayWater < Math.round(goal.getDailyWaterTargetMl() * 0.5)) {
            createDailyNotificationOnce(userId, "Hydration", "Hydration Reminder",
                    "You are currently at " + todayWater + " ml out of your " + goal.getDailyWaterTargetMl() + " ml target today.",
                    today);
        }
    }

    private void createDailyNotificationOnce(int userId, String category, String title,
                                             String message, String today) throws SQLException {
        if (!notificationDAO.existsWithTitleToday(userId, title, today)) {
            notificationDAO.addNotification(userId, category, title, message);
        }
    }

    public ArrayList<NotificationItem> getNotifications(int userId) throws SQLException {
        return notificationDAO.findByUser(userId, 30);
    }

    public int countUnreadNotifications(int userId) throws SQLException {
        return notificationDAO.countUnread(userId);
    }

    public boolean markNotificationRead(int notificationId, int userId) throws SQLException {
        return notificationDAO.markAsRead(notificationId, userId);
    }

    public void markAllNotificationsRead(int userId) throws SQLException {
        notificationDAO.markAllAsRead(userId);
    }

    public boolean deleteNotification(int notificationId, int userId) throws SQLException {
        return notificationDAO.deleteNotification(notificationId, userId);
    }

    // --- User Settings ---
    public UserSettings getSettings(int userId) throws SQLException {
        return userSettingsDAO.findOrCreate(userId);
    }

    public UserSettings updateSettings(UserSettings settings) throws SQLException {
        return userSettingsDAO.saveOrUpdate(settings);
    }

    // --- Workout Planner (with backend tier enforcement) ---
    public WorkoutPlan addPlan(User user, String dayOfWeek, String workoutName, String workoutType,
                               int plannedMinutes, String intensity, String notes) throws SQLException {
        int existingCount = workoutPlanDAO.countByUser(user.getUserId());
        if (existingCount >= user.getMaxWorkoutPlansAllowed()) {
            throw new SecurityException("Regular members can save up to " + user.getMaxWorkoutPlansAllowed()
                    + " planned workouts. Upgrade to Premium membership in Profile/Settings for expanded planning.");
        }
        WorkoutPlan plan = new WorkoutPlan(user.getUserId(), dayOfWeek, workoutName, workoutType,
                plannedMinutes, intensity, notes);
        return workoutPlanDAO.addPlan(plan);
    }

    public WorkoutPlan addPlan(int userId, String dayOfWeek, String workoutType,
                               int plannedMinutes, String intensity, String notes) throws SQLException {
        WorkoutPlan plan = new WorkoutPlan(userId, dayOfWeek, workoutType, workoutType,
                plannedMinutes, intensity, notes);
        return workoutPlanDAO.addPlan(plan);
    }

    public boolean updatePlan(int userId, int planId, String dayOfWeek, String workoutName, String workoutType,
                              int plannedMinutes, String intensity, String notes) throws SQLException {
        WorkoutPlan plan = new WorkoutPlan(userId, dayOfWeek, workoutName, workoutType,
                plannedMinutes, intensity, notes);
        plan.setPlanId(planId);
        return workoutPlanDAO.updatePlan(plan);
    }

    public ArrayList<WorkoutPlan> getPlans(int userId) throws SQLException {
        return workoutPlanDAO.findByUser(userId);
    }

    public Workout completePlan(User user, int planId) throws SQLException {
        WorkoutPlan plan = workoutPlanDAO.findById(planId, user.getUserId());
        if (plan == null) {
            throw new IllegalArgumentException("Workout plan not found.");
        }
        String today = LocalDate.now().toString();
        String note = plan.getNotes() == null || plan.getNotes().isBlank()
                ? "Completed from " + plan.getDayOfWeek() + " plan"
                : plan.getNotes();
        Workout created = workoutService.addWorkout(
                user, plan.getWorkoutName(), plan.getWorkoutType(), plan.getPlannedMinutes(), today,
                plan.getIntensity(), 0, 0.0, note
        );
        workoutPlanDAO.markCompleted(planId, user.getUserId(), today);
        return created;
    }

    public boolean deletePlan(int planId, int userId) throws SQLException {
        return workoutPlanDAO.deletePlan(planId, userId);
    }

    // --- Sleep & Recovery (Neutral Observational Insights) ---
    public SleepLog addSleepLog(int userId, String sleepDate, double hoursSlept,
                                int qualityRating, String notes) throws SQLException {
        if (!ValidationUtils.isValidDate(sleepDate)) {
            throw new IllegalArgumentException("Sleep date must be in YYYY-MM-DD format.");
        }
        SleepLog log = new SleepLog(userId, sleepDate, hoursSlept, qualityRating, notes);
        return sleepDAO.addSleepLog(log);
    }

    public ArrayList<SleepLog> getRecentSleepLogs(int userId) throws SQLException {
        return sleepDAO.findRecentByUser(userId, 20);
    }

    public double getWeeklyAverageSleepHours(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return sleepDAO.getAverageHoursLast7Days(userId, today.minusDays(6).toString(), today.toString());
    }

    public Map<String, Double> getSleepSeries(int userId, LocalDate start, LocalDate end) throws SQLException {
        Map<String, Double> raw = sleepDAO.getDailyHoursBetween(userId, start.toString(), end.toString());
        Map<String, Double> filled = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = cursor.toString();
            filled.put(key, raw.getOrDefault(key, 0.0));
            cursor = cursor.plusDays(1);
        }
        return filled;
    }

    public Map<String, Object> getSleepInsights(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        double avgHours = sleepDAO.getAverageHoursLast7Days(userId, start.toString(), today.toString());
        double avgQuality = sleepDAO.getAverageQualityBetween(userId, start.toString(), today.toString());
        ArrayList<SleepLog> recent = sleepDAO.findRecentByUser(userId, 7);

        int loggedNights = recent.size();
        int sevenPlusNights = 0;
        double minHours = loggedNights > 0 ? recent.get(0).getHoursSlept() : 0.0;
        double maxHours = loggedNights > 0 ? recent.get(0).getHoursSlept() : 0.0;
        for (SleepLog s : recent) {
            if (s.getHoursSlept() >= 7.0) {
                sevenPlusNights++;
            }
            minHours = Math.min(minHours, s.getHoursSlept());
            maxHours = Math.max(maxHours, s.getHoursSlept());
        }

        double spread = loggedNights > 1 ? Math.round((maxHours - minHours) * 10.0) / 10.0 : 0.0;
        String summary;
        if (loggedNights == 0) {
            summary = "Log nightly sleep entries to observe your weekly rest patterns and duration consistency.";
        } else if (spread <= 1.5 && avgHours >= 7.0) {
            summary = "Your recent logged sleep duration stays within a steady " + spread + "-hour band with an average of " + avgHours + " hrs.";
        } else {
            summary = "Recent logged sleep ranges between " + minHours + " and " + maxHours + " hours (" + sevenPlusNights + " of " + loggedNights + " logged nights at 7+ hours).";
        }

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("weeklyAvgHours", avgHours);
        insights.put("weeklyAvgQuality", avgQuality);
        insights.put("loggedNights", loggedNights);
        insights.put("sevenPlusNights", sevenPlusNights);
        insights.put("durationSpread", spread);
        insights.put("consistencySummary", summary);
        return insights;
    }

    public boolean deleteSleepLog(int sleepId, int userId) throws SQLException {
        return sleepDAO.deleteSleepLog(sleepId, userId);
    }

    // --- Weight History ---
    public WeightEntry logWeightEntry(User user, double weightKg, String date) throws SQLException {
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
        }
        WeightEntry entry = weightHistoryDAO.addEntry(new WeightEntry(user.getUserId(), weightKg, date));
        user.setWeightKg(weightKg);
        userDAO.updateProfile(user);
        return entry;
    }

    public boolean updateWeightEntry(int userId, int weightId, double weightKg, String date) throws SQLException {
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
        }
        WeightEntry entry = new WeightEntry(userId, weightKg, date);
        entry.setWeightId(weightId);
        return weightHistoryDAO.updateEntry(entry);
    }

    public boolean deleteWeightEntry(int weightId, int userId) throws SQLException {
        return weightHistoryDAO.deleteEntry(weightId, userId);
    }

    public ArrayList<WeightEntry> getWeightHistory(int userId) throws SQLException {
        return weightHistoryDAO.findByUserAscending(userId, 50);
    }

    public ArrayList<WeightEntry> getWeightHistoryFiltered(int userId, String startDate, String endDate) throws SQLException {
        return weightHistoryDAO.findByUserFiltered(userId, startDate, endDate);
    }

    // --- Personalized Recommendations ---
    public ArrayList<Map<String, Object>> generateRecommendations(User user) throws SQLException {
        ArrayList<Workout> weeklyWorkouts = workoutService.getWorkoutsLast7Days(user.getUserId());
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        for (Workout w : weeklyWorkouts) {
            typeCounts.merge(w.getWorkoutType(), 1, Integer::sum);
        }

        String pref = user.getActivityPreference() == null ? "Balanced" : user.getActivityPreference();
        ArrayList<Map<String, Object>> recs = new ArrayList<>();

        switch (pref) {
            case "Cardio & Endurance" -> {
                recs.add(buildRec(user, "Tempo Run", "Running", 30, "Moderate", "Builds aerobic endurance and supports calorie targets."));
                recs.add(buildRec(user, "Endurance Pool Laps", "Swimming", 35, "Moderate", "Full-body low-impact cardiovascular conditioning."));
                recs.add(buildRec(user, "Interval Conditioning", "HIIT", 20, "High", "Short high-intensity intervals to boost aerobic capacity."));
            }
            case "Strength & Muscle" -> {
                recs.add(buildRec(user, "Upper/Lower Resistance", "Strength Training", 45, "High", "Targets progressive resistance and lean muscle maintenance."));
                recs.add(buildRec(user, "Active Recovery Walk", "Walking", 30, "Moderate", "Boosts daily step count without adding joint fatigue."));
                recs.add(buildRec(user, "Core Mobility Flow", "Yoga", 25, "Moderate", "Improves core stability and post-lifting flexibility."));
            }
            case "Mindfulness & Flexibility" -> {
                recs.add(buildRec(user, "Mindful Mobility Flow", "Yoga", 35, "Moderate", "Aligned with your preference for mobility, balance, and breath."));
                recs.add(buildRec(user, "Outdoor Brisk Walk", "Walking", 35, "Low", "Gentle outdoor movement to reach daily step targets."));
                recs.add(buildRec(user, "Rhythmic Recovery Swim", "Swimming", 30, "Low", "Joint-friendly rhythmic lap swimming for calm recovery."));
            }
            case "Weight Management" -> {
                recs.add(buildRec(user, "Metabolic Intervals", "HIIT", 25, "High", "Maximizes energy expenditure and post-exercise calorie burn."));
                recs.add(buildRec(user, "Steady State Run", "Running", 30, "Moderate", "Steady cardiovascular calorie burn for daily targets."));
                recs.add(buildRec(user, "Daily Step Walk", "Walking", 45, "Moderate", "Sustainable daily step booster with steady output."));
            }
            default -> {
                if (typeCounts.getOrDefault("Strength Training", 0) == 0) {
                    recs.add(buildRec(user, "Full-Body Resistance", "Strength Training", 35, "Moderate", "No strength sessions logged this week; add resistance work."));
                } else {
                    recs.add(buildRec(user, "Pool Conditioning", "Swimming", 30, "Moderate", "Low-impact full-body variety to complement your routine."));
                }
                if (typeCounts.getOrDefault("Yoga", 0) == 0) {
                    recs.add(buildRec(user, "Evening Recovery Flow", "Yoga", 25, "Low", "Add a mobility and recovery session to balance weekly training."));
                } else {
                    recs.add(buildRec(user, "Interval Sprint Block", "HIIT", 20, "High", "Time-efficient interval conditioning for weekly cardio."));
                }
                recs.add(buildRec(user, "Endurance Ride", "Cycling", 35, "Moderate", "Great outdoor or stationary endurance session."));
            }
        }

        if (user.hasExtendedInsightsAccess()) {
            recs.add(buildRec(user, "Custom Circuit Session", "Custom", 30, "Moderate",
                    "Premium tailored cross-training circuit calibrated to your " + user.getWeightKg() + " kg profile."));
        }
        return recs;
    }

    private Map<String, Object> buildRec(User user, String workoutName, String workoutType, int durationMinutes,
                                         String intensity, String reason) {
        Workout sample = workoutService.createWorkoutInstance(
                workoutName, workoutType, user.getUserId(), durationMinutes, LocalDate.now().toString(),
                intensity, 0, 0.0, "", user.getWeightKg()
        );
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("workoutName", sample.getWorkoutName());
        rec.put("workoutType", sample.getWorkoutType());
        rec.put("durationMinutes", durationMinutes);
        rec.put("intensity", intensity);
        rec.put("estimatedCalories", sample.calculateCalories(user.getWeightKg()));
        rec.put("reason", reason);
        return rec;
    }
}
