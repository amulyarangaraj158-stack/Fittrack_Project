package com.fittrack.model;

/**
 * Encapsulated model for daily sleep and recovery tracking.
 */
public class SleepLog {

    private int sleepId;
    private int userId;
    private String sleepDate;
    private double hoursSlept;
    private int qualityRating;
    private String notes;

    public SleepLog() {
    }

    public SleepLog(int userId, String sleepDate, double hoursSlept, int qualityRating, String notes) {
        this.userId = userId;
        this.sleepDate = sleepDate;
        setHoursSlept(hoursSlept);
        setQualityRating(qualityRating);
        this.notes = notes == null ? "" : notes.trim();
    }

    public SleepLog(int sleepId, int userId, String sleepDate, double hoursSlept, int qualityRating, String notes) {
        this.sleepId = sleepId;
        this.userId = userId;
        this.sleepDate = sleepDate;
        this.hoursSlept = hoursSlept;
        this.qualityRating = qualityRating;
        this.notes = notes;
    }

    public int getSleepId() {
        return sleepId;
    }

    public void setSleepId(int sleepId) {
        this.sleepId = sleepId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSleepDate() {
        return sleepDate;
    }

    public void setSleepDate(String sleepDate) {
        this.sleepDate = sleepDate;
    }

    public double getHoursSlept() {
        return hoursSlept;
    }

    public void setHoursSlept(double hoursSlept) {
        if (hoursSlept <= 0 || hoursSlept > 24) {
            throw new IllegalArgumentException("Hours slept must be between 0.5 and 24.");
        }
        this.hoursSlept = Math.round(hoursSlept * 10.0) / 10.0;
    }

    public int getQualityRating() {
        return qualityRating;
    }

    public void setQualityRating(int qualityRating) {
        if (qualityRating < 1 || qualityRating > 5) {
            throw new IllegalArgumentException("Sleep quality must be rated 1 to 5.");
        }
        this.qualityRating = qualityRating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }
}
