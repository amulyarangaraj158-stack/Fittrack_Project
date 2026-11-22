package com.fittrack.model;

/**
 * Represents a single water consumption record logged by a user.
 * Demonstrates OOP Encapsulation and Constructor Overloading.
 */
public class WaterIntake {

    private int waterId;
    private int userId;
    private int amountMl;
    private String intakeDate;
    private String intakeTime;
    private String note;

    public WaterIntake() {
    }

    public WaterIntake(int userId, int amountMl, String intakeDate, String intakeTime, String note) {
        this.userId = userId;
        setAmountMl(amountMl);
        this.intakeDate = intakeDate;
        this.intakeTime = intakeTime;
        this.note = note == null ? "" : note.trim();
    }

    public WaterIntake(int waterId, int userId, int amountMl, String intakeDate, String intakeTime, String note) {
        this.waterId = waterId;
        this.userId = userId;
        this.amountMl = amountMl;
        this.intakeDate = intakeDate;
        this.intakeTime = intakeTime;
        this.note = note;
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
