package com.fittrack.model;

/**
 * RegularUser subclass extending the base User class.
 * Demonstrates OOP Inheritance and Method Overriding.
 */
public class RegularUser extends User {

    public RegularUser() {
        super();
        setAccountType(ACCOUNT_REGULAR);
    }

    public RegularUser(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash);
        setAccountType(ACCOUNT_REGULAR);
    }

    public RegularUser(int userId, String fullName, String email, String passwordHash,
                       int age, String gender, double heightCm, double weightKg, String createdAt) {
        super(userId, fullName, email, passwordHash, ACCOUNT_REGULAR, age, gender, heightCm, weightKg, createdAt);
    }

    @Override
    public String getMembershipBadge() {
        return "Regular Member";
    }

    @Override
    public String getMembershipBenefits() {
        return "Daily activity logging, workout tracking, water intake goals, and weekly reports.";
    }
}
