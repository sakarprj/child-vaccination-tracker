package com.hospital.vaccination.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Thin wrapper around jBCrypt so the rest of the app never touches
 * the BCrypt API directly. If we ever swap hashing libraries, this
 * is the only file that changes.
 */
public final class PasswordUtil {

    /** BCrypt work factor. 10 = ~100ms per hash on a modern laptop. */
    private static final int WORK_FACTOR = 10;

    private PasswordUtil() { }

    /** @return a BCrypt hash of {@code plain}, safe to store in the DB. */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * @return true if {@code plain} matches the previously-hashed
     *         {@code hashed}. Returns false (rather than throwing) if
     *         {@code hashed} is malformed, so a corrupted row in the
     *         DB can't crash the login screen.
     */
    public static boolean verify(String plain, String hashed) {
        if (plain == null || hashed == null || hashed.isEmpty()) return false;
        try {
            return BCrypt.checkpw(plain, hashed);
        } catch (IllegalArgumentException e) {
            // Not a valid BCrypt hash (e.g. the placeholder seed value)
            return false;
        }
    }

    /**
     * True if the given DB value doesn't look like a real BCrypt hash.
     * Used by {@link com.hospital.vaccination.config.DBBootstrap} to
     * detect the placeholder seed rows and rewrite them on first run.
     */
    public static boolean looksLikeValidHash(String hashed) {
        if (hashed == null || hashed.length() < 60) return false;
        // Quick BCrypt self-check: verify against a dummy string. If it
        // throws IllegalArgumentException, the hash is malformed.
        try {
            BCrypt.checkpw("probe", hashed);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
