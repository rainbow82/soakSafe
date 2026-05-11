package com.shannon.soaksafe.security;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2-HMAC-SHA256 password storage. Encoded form:
 * {@code SS1$<iterations>$<saltBase64>$<hashBase64>}
 * <p>
 * Values that do not use this prefix are treated as legacy plaintext (for one-time upgrade on login).
 */
public final class PasswordHasher {

    private static final String PREFIX = "SS1$";
    private static final int SALT_BYTES = 16;
    private static final int DERIVED_KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 200_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static boolean isModernStoredForm(@NonNull String storedPassword) {
        if (!storedPassword.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = storedPassword.split("\\$", -1);
        return parts.length == 4 && "SS1".equals(parts[0]);
    }

    /**
     * @return encoded string suitable for {@link #verify(String, String)}
     */
    @NonNull
    public static String hash(@NonNull String plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword, salt, PBKDF2_ITERATIONS);
        return PREFIX + PBKDF2_ITERATIONS + '$'
                + Base64.getEncoder().encodeToString(salt) + '$'
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a candidate password against a stored value (modern encoding or legacy plaintext).
     */
    public static boolean verify(@NonNull String storedPassword, @NonNull String plainPassword) {
        if (isModernStoredForm(storedPassword)) {
            return verifyModern(storedPassword, plainPassword);
        }
        return slowEqualsLegacy(storedPassword, plainPassword);
    }

    private static boolean verifyModern(@NonNull String storedPassword, @NonNull String plainPassword) {
        String[] parts = storedPassword.split("\\$", -1);
        if (parts.length != 4 || !"SS1".equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
            if (iterations < 10_000) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = pbkdf2(plainPassword, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static boolean slowEqualsLegacy(@NonNull String stored, @NonNull String plain) {
        return stored.equals(plain);
    }

    @NonNull
    private static byte[] pbkdf2(@NonNull String plainPassword, @NonNull byte[] salt, int iterations) {
        char[] passwordChars = plainPassword.toCharArray();
        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, DERIVED_KEY_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }
}
