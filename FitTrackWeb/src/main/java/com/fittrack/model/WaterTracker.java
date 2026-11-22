package com.fittrack.model;

/**
 * WaterTracker model & helper representing a hydration log entry
 * and remaining water calculation logic.
 */
public class WaterTracker {

    private int waterId;
    private int userId;
    private int amountMl;
    private String intakeDate;
    private String intakeTime;
    private String note;

    public WaterTracker() {
    }

    public WaterTracker(int userId, int amountMl, String intakeDate, String intakeTime, String note) {
        this.userId = userId;
        setAmountMl(amountMl);
        this.intakeDate = intakeDate;
        this.intakeTime = intakeTime;
        this.note = note == null ? "" : note.trim();
    }

    public WaterTracker(int waterId, int userId, int amountMl, String intakeDate, String intakeTime, String note) {
        this.waterId = waterId;
        this.userId = userId;
        this.amountMl = amountMl;
        this.intakeDate = intakeDate;
        this.intakeTime = intakeTime;
        this.note = note;
    }

    public static int calculateRemainingMl(int consumedMl, int targetMl) {
        return Math.max(0, targetMl - consumedMl);
    }

    public static int calculateHydrationPercent(int consumedMl, int targetMl) {
        if (targetMl <= 0) {
            return 0;
        }
        return (int) Math.max(0, Math.min(100, Math.round((consumedMl * 100.0) / targetMl)));
    }

    public int getWaterId() {
        return waterId;
    }

    public void setWaterId(int waterId) {
        this.waterId = waterId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAmountMl() {
        return amountMl;
    }

    public void setAmountMl(int amountMl) {
        if (amountMl <= 0 || amountMl > 5000) {
            throw new IllegalArgumentException("Water amount must be between 1 ml and 5,000 ml.");
        }
        this.amountMl = amountMl;
    }

    public String getIntakeDate() {
        return intakeDate;
    }

    public void setIntakeDate(String intakeDate) {
        this.intakeDate = intakeDate;
    }

    public String getIntakeTime() {
        return intakeTime;
    }

    public void setIntakeTime(String intakeTime) {
        this.intakeTime = intakeTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
    }
}
