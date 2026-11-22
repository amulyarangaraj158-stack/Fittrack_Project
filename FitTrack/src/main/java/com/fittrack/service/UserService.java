package com.fittrack.service;

import com.fittrack.dao.UserDAO;
import com.fittrack.model.PremiumUser;
import com.fittrack.model.RegularUser;
import com.fittrack.model.User;
import com.fittrack.util.PasswordUtils;
import com.fittrack.util.ValidationUtils;

import java.sql.SQLException;

/**
 * Service layer for user authentication, registration, and profile management.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public User register(String fullName, String email, String plainPassword, String accountType,
                         int age, String gender, double heightCm, double weightKg) throws SQLException {
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

        String passwordHash = PasswordUtils.hashPassword(plainPassword);

        // Polymorphic User instantiation based on chosen account type
        User newUser = User.ACCOUNT_PREMIUM.equalsIgnoreCase(accountType)
                ? new PremiumUser(fullName, email, passwordHash)
                : new RegularUser(fullName, email, passwordHash);

        newUser.setAge(age);
        newUser.setGender(gender);
        newUser.setHeightCm(heightCm);
        newUser.setWeightKg(weightKg);

        return userDAO.createUserWithDefaultGoal(newUser);
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
        return user;
    }

    public User updateProfile(User user, String fullName, String accountType,
                              int age, String gender, double heightCm, double weightKg) throws SQLException {
        user.setFullName(fullName);
        user.setAccountType(accountType);
        user.setAge(age);
        user.setGender(gender);
        user.setHeightCm(heightCm);
        user.setWeightKg(weightKg);

        boolean updated = userDAO.updateProfile(user);
        if (!updated) {
            throw new SQLException("Unable to update user profile.");
        }
        return userDAO.findById(user.getUserId());
    }

    public User refreshUser(int userId) throws SQLException {
        return userDAO.findById(userId);
    }
}
