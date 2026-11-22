package com.fittrack.model;

/**
 * PremiumUser subclass extending the base User class.
 * Demonstrates OOP Inheritance and Method Overriding.
 */
public class PremiumUser extends User {

    public PremiumUser() {
        super();
        setAccountType(ACCOUNT_PREMIUM);
    }

    public PremiumUser(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash);
        setAccountType(ACCOUNT_PREMIUM);
    }

    public PremiumUser(int userId, String fullName, String email, String passwordHash,
                       int age, String gender, double heightCm, double weightKg, String createdAt) {
        super(userId, fullName, email, passwordHash, ACCOUNT_PREMIUM, age, gender, heightCm, weightKg, createdAt);
    }

    @Override
    public String getMembershipBadge() {
        return "Premium Pro Member";
    }

    @Override
    public String getMembershipBenefits() {
        return "Full analytics, CSV workout report export, multi-metric goal tracking, and priority insights.";
    }
}
