package com.fittrack.model;

/**
 * Represents a timestamped body weight measurement for trend tracking.
 */
public class WeightEntry {

    private int weightId;
    private int userId;
    private double weightKg;
    private String recordedDate;

    public WeightEntry() {
    }

    public WeightEntry(int userId, double weightKg, String recordedDate) {
        this.userId = userId;
        setWeightKg(weightKg);
        this.recordedDate = recordedDate;
    }

    public WeightEntry(int weightId, int userId, double weightKg, String recordedDate) {
        this.weightId = weightId;
        this.userId = userId;
        this.weightKg = weightKg;
        this.recordedDate = recordedDate;
    }

    public int getWeightId() {
        return weightId;
    }

    public void setWeightId(int weightId) {
        this.weightId = weightId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        if (weightKg < 20 || weightKg > 350) {
            throw new IllegalArgumentException("Weight must be between 20 kg and 350 kg.");
        }
        this.weightKg = Math.round(weightKg * 10.0) / 10.0;
    }

    public String getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(String recordedDate) {
        this.recordedDate = recordedDate;
    }
}
