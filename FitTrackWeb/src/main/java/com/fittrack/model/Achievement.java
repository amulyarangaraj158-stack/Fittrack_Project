package com.fittrack.model;

import com.fittrack.interfaces.Awardable;

/**
 * Achievement milestone unlocked or tracked for a user, implementing Awardable.
 */
public class Achievement implements Awardable {

    private int achievementId;
    private int userId;
    private String badgeCode;
    private String title;
    private String description;
    private String unlockedAt;
    private boolean unlocked;
    private int progressPercent;
    private int currentValue;
    private int targetValue;

    public Achievement() {
    }

    public Achievement(int userId, String badgeCode, String title, String description, String unlockedAt) {
        this.userId = userId;
        this.badgeCode = badgeCode;
        this.title = title;
        this.description = description;
        this.unlockedAt = unlockedAt;
        this.unlocked = unlockedAt != null && !unlockedAt.isBlank();
        this.progressPercent = this.unlocked ? 100 : 0;
    }

    public Achievement(int achievementId, int userId, String badgeCode, String title, String description, String unlockedAt) {
        this.achievementId = achievementId;
        this.userId = userId;
        this.badgeCode = badgeCode;
        this.title = title;
        this.description = description;
        this.unlockedAt = unlockedAt;
        this.unlocked = unlockedAt != null && !unlockedAt.isBlank();
        this.progressPercent = this.unlocked ? 100 : 0;
    }

    public int getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String getBadgeCode() {
        return badgeCode;
    }

    public void setBadgeCode(String badgeCode) {
        this.badgeCode = badgeCode;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(String unlockedAt) {
        this.unlockedAt = unlockedAt;
        this.unlocked = unlockedAt != null && !unlockedAt.isBlank();
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int currentValue) {
        this.currentValue = Math.max(0, currentValue);
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = Math.max(1, targetValue);
    }
}
