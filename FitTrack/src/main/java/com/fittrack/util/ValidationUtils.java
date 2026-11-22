package com.fittrack.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Input validation helper methods shared across UI and Service layers.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtils() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate.parse(dateStr.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isNonEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static int parsePositiveInt(String text, String fieldName) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid whole number.");
        }
    }

    public static int parseNonNegativeInt(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid whole number.");
        }
    }

    public static double parsePositiveDouble(String text, String fieldName) {
        try {
            double value = Double.parseDouble(text.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    public static double parseNonNegativeDouble(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }
}
