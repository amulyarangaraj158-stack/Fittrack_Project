package com.fittrack.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Provides salted SHA-256 password hashing and verification.
 * Ensures plain-text passwords are never stored in SQLite.
 */
public final class PasswordUtils {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 16;

    private PasswordUtils() {
    }

    /**
     * Generates a random salt and hashes the password using SHA-256.
     * Format: base64Salt$base64Hash
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = computeHash(plainPassword, salt);
        return saltB64 + "$" + hashB64;
    }

    /**
     * Verifies a plain-text password against the stored salted hash.
     */
    public static boolean verifyPassword(String plainPassword, String storedValue) {
        if (plainPassword == null || storedValue == null || !storedValue.contains("$")) {
            return false;
        }
        String[] parts = storedValue.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String computedHash = computeHash(plainPassword, salt);
            return MessageDigest.isEqual(
                    computedHash.getBytes(StandardCharsets.UTF_8),
                    parts[1].getBytes(StandardCharsets.UTF_8)
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String computeHash(String plainPassword, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashed = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available.", e);
        }
    }
}
