package com.fittrack.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing utility using PBKDF2WithHmacSHA256 (with backwards compatibility
 * for legacy salted SHA-256 hashes).
 */
public final class PasswordUtils {

    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final int SALT_LENGTH = 16;

    private PasswordUtils() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String keyB64 = pbkdf2(plainPassword.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
        return PBKDF2_PREFIX + "$" + PBKDF2_ITERATIONS + "$" + saltB64 + "$" + keyB64;
    }

    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || !storedHash.contains("$")) {
            return false;
        }
        if (storedHash.startsWith(PBKDF2_PREFIX + "$")) {
            String[] parts = storedHash.split("\\$", 4);
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                String computed = pbkdf2(plainPassword.toCharArray(), salt, iterations, PBKDF2_KEY_BITS);
                return MessageDigest.isEqual(
                        computed.getBytes(StandardCharsets.UTF_8),
                        parts[3].getBytes(StandardCharsets.UTF_8)
                );
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        // Legacy salted SHA-256 fallback verification
        String[] parts = storedHash.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String computed = legacySha256(plainPassword, salt);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    parts[1].getBytes(StandardCharsets.UTF_8)
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !storedHash.startsWith(PBKDF2_PREFIX + "$");
    }

    private static String pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 password hashing unavailable.", ex);
        }
    }

    private static String legacySha256(String plainPassword, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable.", e);
        }
    }
}
