package com.fittrack.model;

/**
 * Base User class representing a FitTrack application user.
 *
 * Demonstrates OOP Encapsulation (private fields + validated getters/setters),
 * Constructor Overloading, and Method Overloading (calculateBMI).
 */
public class User {

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
    private String createdAt;

    // Default constructor
    public User() {
        this.accountType = ACCOUNT_REGULAR;
        this.age = 20;
        this.gender = "Not Specified";
        this.heightCm = 170.0;
        this.weightKg = 70.0;
    }

    // Overloaded constructor for quick registration
    public User(String fullName, String email, String passwordHash) {
        this();
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Overloaded full constructor for database hydration
    public User(int userId, String fullName, String email, String passwordHash,
                String accountType, int age, String gender,
                double heightCm, double weightKg, String createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
        this.age = age;
        this.gender = gender;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.createdAt = createdAt;
    }

    // Overridable method for polymorphism across RegularUser and PremiumUser
    public String getMembershipBadge() {
        return "Standard Member";
    }

    // Overridable method describing tier capabilities
    public String getMembershipBenefits() {
        return "Core workout tracking, daily goals, and water intake monitoring.";
    }

    /**
     * Calculates Body Mass Index (BMI) as a general informational metric
     * using this user's stored height and weight.
     */
    public double calculateBMI() {
        return calculateBMI(this.heightCm, this.weightKg);
    }

    /**
     * Overloaded method: calculates BMI from explicit height (cm) and weight (kg).
     */
    public double calculateBMI(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) {
            return 0.0;
        }
        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);
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

    // Encapsulated Getters and Setters
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
            throw new IllegalArgumentException("Full name cannot be empty.");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty.");
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
        this.accountType = (accountType == null || accountType.isBlank()) ? ACCOUNT_REGULAR : accountType.toUpperCase();
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
            throw new IllegalArgumentException("Height must be between 50 cm and 260 cm.");
        }
        this.heightCm = heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        if (weightKg < 20 || weightKg > 350) {
            throw new IllegalArgumentException("Weight must be between 20 kg and 350 kg.");
        }
        this.weightKg = weightKg;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
