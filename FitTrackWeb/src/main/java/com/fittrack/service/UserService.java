package com.fittrack.service;

import com.fittrack.dao.UserDAO;
import com.fittrack.dao.WeightHistoryDAO;
import com.fittrack.model.PremiumUser;
import com.fittrack.model.RegularUser;
import com.fittrack.model.User;
import com.fittrack.model.WeightEntry;
import com.fittrack.util.PasswordUtils;
import com.fittrack.util.ValidationUtils;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Service layer for user registration, login (with PBKDF2 auto-rehash), password management,
 * account deletion, and profile updates.
 */
public class UserService {

    private final UserDAO userDAO;
    private final WeightHistoryDAO weightHistoryDAO;

    public UserService() {
        this.userDAO = new UserDAO();
        this.weightHistoryDAO = new WeightHistoryDAO();
    }

    public User register(String fullName, String email, String plainPassword, String accountType,
                         int age, String gender, double heightCm, double weightKg,
                         String activityPreference) throws SQLException {
        if (!ValidationUtils.isNonEmpty(fullName)) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (!ValidationUtils.isValidPassword(plainPassword)) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (userDAO.emailExists(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        String hash = PasswordUtils.hashPassword(plainPassword);
        User user = User.ACCOUNT_PREMIUM.equalsIgnoreCase(accountType)
                ? new PremiumUser(fullName, email, hash)
                : new RegularUser(fullName, email, hash);

        user.setAge(age);
        user.setGender(gender);
        user.setHeightCm(heightCm);
        user.setWeightKg(weightKg);
        user.setActivityPreference(activityPreference);

        return userDAO.createUserWithDefaultGoal(user);
    }

    public User login(String email, String plainPassword) throws SQLException {
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        User user = userDAO.findByEmail(email);
        if (user == null || !PasswordUtils.verifyPassword(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        // Upgrade legacy SHA-256 hashes transparently to PBKDF2WithHmacSHA256
        if (PasswordUtils.needsRehash(user.getPasswordHash())) {
            String upgraded = PasswordUtils.hashPassword(plainPassword);
            userDAO.updatePasswordHash(user.getUserId(), upgraded);
            user.setPasswordHash(upgraded);
        }
        return user;
    }

    public User updateProfile(User user, String fullName, String accountType,
                              int age, String gender, double heightCm, double weightKg,
                              String activityPreference) throws SQLException {
        double oldWeight = user.getWeightKg();
        user.setFullName(fullName);
        user.setAccountType(accountType);
        user.setAge(age);
        user.setGender(gender);
        user.setHeightCm(heightCm);
        user.setWeightKg(weightKg);
        user.setActivityPreference(activityPreference);

        if (!userDAO.updateProfile(user)) {
            throw new SQLException("Unable to update profile.");
        }
        if (Math.abs(oldWeight - user.getWeightKg()) >= 0.05) {
            weightHistoryDAO.addEntry(new WeightEntry(user.getUserId(), user.getWeightKg(), LocalDate.now().toString()));
        }
        return userDAO.findById(user.getUserId());
    }

    public void changePassword(User user, String currentPassword, String newPassword) throws SQLException {
        if (currentPassword == null || !PasswordUtils.verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (!ValidationUtils.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }
        String newHash = PasswordUtils.hashPassword(newPassword);
        if (!userDAO.updatePasswordHash(user.getUserId(), newHash)) {
            throw new SQLException("Failed to update password.");
        }
    }

    public boolean deleteAccount(int userId, String password) throws SQLException {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User account not found.");
        }
        if (password == null || !PasswordUtils.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Password confirmation failed.");
        }
        return userDAO.deleteUser(userId);
    }

    public User getUserById(int userId) throws SQLException {
        return userDAO.findById(userId);
    }
}
