package com.fittrack.model;

/**
 * Encapsulated model for per-user UI theme and notification preferences.
 */
public class UserSettings {

    private int settingId;
    private int userId;
    private String themeMode;
    private boolean workoutReminders;
    private boolean waterReminders;
    private boolean goalAlerts;
    private boolean achievementAlerts;
    private boolean weeklyDigest;
    private String updatedAt;

    public UserSettings() {
        this.themeMode = "light";
        this.workoutReminders = true;
        this.waterReminders = true;
        this.goalAlerts = true;
        this.achievementAlerts = true;
        this.weeklyDigest = true;
    }

    public UserSettings(int settingId, int userId, String themeMode,
                        boolean workoutReminders, boolean waterReminders,
                        boolean goalAlerts, boolean achievementAlerts,
                        boolean weeklyDigest, String updatedAt) {
        this.settingId = settingId;
        this.userId = userId;
        setThemeMode(themeMode);
        this.workoutReminders = workoutReminders;
        this.waterReminders = waterReminders;
        this.goalAlerts = goalAlerts;
        this.achievementAlerts = achievementAlerts;
        this.weeklyDigest = weeklyDigest;
        this.updatedAt = updatedAt;
    }

    public int getSettingId() {
        return settingId;
    }

    public void setSettingId(int settingId) {
        this.settingId = settingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = "dark".equalsIgnoreCase(themeMode) ? "dark" : "light";
    }

    public boolean isWorkoutReminders() {
        return workoutReminders;
    }

    public void setWorkoutReminders(boolean workoutReminders) {
        this.workoutReminders = workoutReminders;
    }

    public boolean isWaterReminders() {
        return waterReminders;
    }

    public void setWaterReminders(boolean waterReminders) {
        this.waterReminders = waterReminders;
    }

    public boolean isGoalAlerts() {
        return goalAlerts;
    }

    public void setGoalAlerts(boolean goalAlerts) {
        this.goalAlerts = goalAlerts;
    }

    public boolean isAchievementAlerts() {
        return achievementAlerts;
    }

    public void setAchievementAlerts(boolean achievementAlerts) {
        this.achievementAlerts = achievementAlerts;
    }

    public boolean isWeeklyDigest() {
        return weeklyDigest;
    }

    public void setWeeklyDigest(boolean weeklyDigest) {
        this.weeklyDigest = weeklyDigest;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
