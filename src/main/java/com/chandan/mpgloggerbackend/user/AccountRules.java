package com.chandan.mpgloggerbackend.user;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Rules for sign-up and account details. Kept free of Spring/JPA so it can be unit tested on its own.
 * The React sign-up form checks the same rules before sending.
 */
public final class AccountRules {

    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 30;
    public static final int NAME_MAX = 50;
    public static final int PASSWORD_MIN = 8;
    /** BCrypt only reads the first 72 bytes, so longer passwords are refused rather than silently cut. */
    public static final int PASSWORD_MAX_BYTES = 72;

    private static final Pattern USERNAME_CHARS = Pattern.compile("[a-z0-9._]+");

    private AccountRules() {
    }

    /** Usernames are case-insensitive: "Chandan " and "chandan" are the same account. */
    public static String normalizeUsername(String username) {
        return username == null ? "" : username.strip().toLowerCase();
    }

    /** Expects a username that has already been through normalizeUsername. */
    public static void validateUsername(String username) {
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            throw new InvalidAccountException(
                    "Username must be " + USERNAME_MIN + " to " + USERNAME_MAX + " characters.");
        }
        if (!USERNAME_CHARS.matcher(username).matches()) {
            throw new InvalidAccountException("Username can only use letters, numbers, dots and underscores.");
        }
    }

    /** Returns the name with surrounding spaces removed. */
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

    public static void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new InvalidAccountException("Password must be at least " + PASSWORD_MIN + " characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new InvalidAccountException("Password is too long.");
        }
    }
}
