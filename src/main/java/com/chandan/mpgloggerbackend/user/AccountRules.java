package com.chandan.mpgloggerbackend.user;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Rules for sign-up and account details. Kept free of Spring/JPA so it can be unit tested on its own.
 * The React sign-up form checks the same rules before sending.
 */
public final class AccountRules {

    /** Shortest allowed username. */
    public static final int USERNAME_MIN = 3;
    /** Longest allowed username (also the database column size). */
    public static final int USERNAME_MAX = 30;
    /** Longest allowed name (also the database column size). */
    public static final int NAME_MAX = 50;
    /** Shortest allowed password. */
    public static final int PASSWORD_MIN = 8;
    /** BCrypt only reads the first 72 bytes, so longer passwords are refused rather than silently cut. */
    public static final int PASSWORD_MAX_BYTES = 72;

    private static final Pattern USERNAME_CHARS = Pattern.compile("[a-z0-9._]+");

    /** Only static methods; never instantiated. */
    private AccountRules() {
    }

    /**
     * Usernames are case-insensitive: "Chandan " and "chandan" are the same account.
     *
     * @param username what was typed, may be null
     * @return the username trimmed and lowercased ("" for null)
     */
    public static String normalizeUsername(String username) {
        return username == null ? "" : username.strip().toLowerCase();
    }

    /**
     * Checks length and allowed characters.
     *
     * @param username a username that has already been through {@link #normalizeUsername}
     * @throws InvalidAccountException if it is too short, too long or has other characters
     */
    public static void validateUsername(String username) {
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            throw new InvalidAccountException(
                    "Username must be " + USERNAME_MIN + " to " + USERNAME_MAX + " characters.");
        }
        if (!USERNAME_CHARS.matcher(username).matches()) {
            throw new InvalidAccountException("Username can only use letters, numbers, dots and underscores.");
        }
    }

    /**
     * Checks that a name is given and not too long.
     *
     * @param name what was typed, may be null
     * @return the name with surrounding spaces removed
     * @throws InvalidAccountException if it is blank or too long
     */
    public static String validateName(String name) {
        String stripped = name == null ? "" : name.strip();
        if (stripped.isEmpty()) {
            throw new InvalidAccountException("Name is required.");
        }
        if (stripped.length() > NAME_MAX) {
            throw new InvalidAccountException("Name must be " + NAME_MAX + " characters or fewer.");
        }
        return stripped;
    }

    /**
     * Checks a new password's length.
     *
     * @param password what was typed, may be null
     * @throws InvalidAccountException if it is shorter than 8 characters or longer than 72 bytes
     */
    public static void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new InvalidAccountException("Password must be at least " + PASSWORD_MIN + " characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new InvalidAccountException("Password is too long.");
        }
    }
}
