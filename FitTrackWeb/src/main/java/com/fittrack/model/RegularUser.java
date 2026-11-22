package com.fittrack.model;

/**
 * RegularUser subclass extending abstract User.
 * Demonstrates Inheritance and Method Overriding.
 */
public class RegularUser extends User {

    public static final int REGULAR_MAX_PLANS = 7;

    public RegularUser() {
        super();
        setAccountType(ACCOUNT_REGULAR);
    }

    public RegularUser(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash);
        setAccountType(ACCOUNT_REGULAR);
    }

    public RegularUser(int userId, String fullName, String email, String passwordHash,
                       int age, String gender, double heightCm, double weightKg,
                       String activityPreference, String createdAt) {
        super(userId, fullName, email, passwordHash, ACCOUNT_REGULAR, age, gender,
                heightCm, weightKg, activityPreference, createdAt);
    }

    @Override
    public String getMembershipBadge() {
        return "Regular Member";
    }

    @Override
    public String getMembershipBenefits() {
        return "Core workout logging, up to 7 weekly planner sessions, hydration & sleep tracking, and 7-day / 30-day reports.";
    }

    @Override
    public boolean hasPremiumAnalyticsAccess() {
        return false;
    }

    @Override
    public int getMaxWorkoutPlansAllowed() {
        return REGULAR_MAX_PLANS;
    }

    @Override
    public boolean hasExtendedInsightsAccess() {
        return false;
    }
}
