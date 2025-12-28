package com.example.silkmall.security;

import java.util.regex.Pattern;

/**
 * Utility helpers for determining whether a password value is already encoded.
 * This prevents double-encoding existing BCrypt hashes when updating user records.
 */
public final class PasswordHashValidator {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile(
            "\\A(?:\\{bcrypt\\})?\\$2[aby]?\\$\\d\\d\\$[./0-9A-Za-z]{53}\\z");

    private PasswordHashValidator() {
    }

    /**
     * Returns true when the supplied password string already matches the structure of a BCrypt hash.
     */
    public static boolean isBcryptHash(String password) {
        return password != null && BCRYPT_PATTERN.matcher(password).matches();
    }
}
