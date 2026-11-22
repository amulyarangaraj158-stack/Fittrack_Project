package com.fittrack.model;

/**
 * Abstract base User class representing a FitTrack member.
 * Demonstrates Abstraction, Encapsulation, Constructor Overloading, and Method Overloading.
 */
public abstract class User {

    public static final String ACCOUNT_REGULAR = "REGULAR";
    public static final String ACCOUNT_PREMIUM = "PREMIUM";

    private int userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private String accountType;
    private int age;
    private String gender;
    private double heightCm;
    private double weightKg;
    private String activityPreference;
    private String createdAt;

    protected User() {
        this.accountType = ACCOUNT_REGULAR;
        this.age = 20;
        this.gender = "Not Specified";
        this.heightCm = 170.0;
        this.weightKg = 70.0;
        this.activityPreference = "Balanced";
    }

    protected User(String fullName, String email, String passwordHash) {
        this();
        setFullName(fullName);
        setEmail(email);
        this.passwordHash = passwordHash;
    }

    protected User(int userId, String fullName, String email, String passwordHash,
                   String accountType, int age, String gender,
                   double heightCm, double weightKg, String activityPreference, String createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
        this.age = age;
        this.gender = gender;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityPreference = (activityPreference == null || activityPreference.isBlank())
                ? "Balanced" : activityPreference;
        this.createdAt = createdAt;
    }

    public abstract String getMembershipBadge();

    public abstract String getMembershipBenefits();

    public abstract boolean hasPremiumAnalyticsAccess();

    public abstract int getMaxWorkoutPlansAllowed();

    public abstract boolean hasExtendedInsightsAccess();

    /**
     * Informational BMI calculation using this user's stored height & weight.
     */
    public double calculateBMI() {
        return calculateBMI(this.heightCm, this.weightKg);
    }

    /**
     * Overloaded BMI calculation method taking explicit height (cm) and weight (kg).
     */
    public double calculateBMI(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) {
            return 0.0;
        }
        double meters = heightCm / 100.0;
        double bmi = weightKg / (meters * meters);
        return Math.round(bmi * 10.0) / 10.0;
    }

    public String getBmiCategory() {
        double bmi = calculateBMI();
        if (bmi <= 0) {
            return "Unavailable";
        } else if (bmi < 18.5) {
            return "Underweight (General Reference)";
        } else if (bmi < 25.0) {
            return "Normal Range (General Reference)";
        } else if (bmi < 30.0) {
            return "Overweight Range (General Reference)";
        } else {
            return "Higher BMI Range (General Reference)";
        }
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = (accountType == null || accountType.isBlank())
                ? ACCOUNT_REGULAR : accountType.trim().toUpperCase();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 10 || age > 120) {
            throw new IllegalArgumentException("Age must be between 10 and 120.");
        }
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = (gender == null || gender.isBlank()) ? "Not Specified" : gender.trim();
    }

    public double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(double heightCm) {
        if (heightCm < 50 || heightCm > 260) {
            throw new IllegalArgumentException("Height must be between 50 and 260 cm.");
        }
        this.heightCm = heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        if (weightKg < 20 || weightKg > 350) {
            throw new IllegalArgumentException("Weight must be between 20 and 350 kg.");
        }
        this.weightKg = weightKg;
    }

    public String getActivityPreference() {
        return activityPreference;
    }

    public void setActivityPreference(String activityPreference) {
        this.activityPreference = (activityPreference == null || activityPreference.isBlank())
                ? "Balanced" : activityPreference.trim();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
