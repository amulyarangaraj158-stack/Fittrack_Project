package com.fittrack.model;

/**
 * PremiumUser subclass extending abstract User.
 * Demonstrates Inheritance and Method Overriding.
 */
public class PremiumUser extends User {

    public static final int PREMIUM_MAX_PLANS = 50;

    public PremiumUser() {
        super();
        setAccountType(ACCOUNT_PREMIUM);
    }

    public PremiumUser(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash);
        setAccountType(ACCOUNT_PREMIUM);
    }

    public PremiumUser(int userId, String fullName, String email, String passwordHash,
                       int age, String gender, double heightCm, double weightKg,
                       String activityPreference, String createdAt) {
        super(userId, fullName, email, passwordHash, ACCOUNT_PREMIUM, age, gender,
                heightCm, weightKg, activityPreference, createdAt);
    }

    @Override
    public String getMembershipBadge() {
        return "Premium Member";
    }

    @Override
    public String getMembershipBenefits() {
        return "Extended 3-month & 6-month analytics, advanced sleep & hydration insights, unlimited workout planning, and tailored recommendations.";
    }

    @Override
    public boolean hasPremiumAnalyticsAccess() {
        return true;
    }

    @Override
    public int getMaxWorkoutPlansAllowed() {
        return PREMIUM_MAX_PLANS;
    }

    @Override
    public boolean hasExtendedInsightsAccess() {
        return true;
    }
}
