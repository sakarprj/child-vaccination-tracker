package com.hospital.vaccination.config;

import com.hospital.vaccination.dao.UserDAO;
import com.hospital.vaccination.util.PasswordUtil;

/**
 * Runs once at application startup.
 *
 * <p>The seed rows in <code>schema.sql</code> contain placeholder
 * (non-BCrypt) password hashes so the SQL file has no dependency on
 * the Java build. On first launch we detect those placeholders and
 * overwrite them with real BCrypt hashes:
 *
 * <ul>
 *   <li><b>admin</b> / admin123</li>
 *   <li><b>nurse1</b> / nurse123</li>
 * </ul>
 *
 * <p>Idempotent: safe to run on every startup. After the first fix,
 * subsequent runs are no-ops because the hashes will pass
 * {@link PasswordUtil#looksLikeValidHash(String)}.
 */
public final class DBBootstrap {

    private DBBootstrap() { }

    public static void run() {
        UserDAO dao = new UserDAO();

        fixIfPlaceholder(dao, "admin",  "admin123");
        fixIfPlaceholder(dao, "nurse1", "nurse123");
    }

    private static void fixIfPlaceholder(UserDAO dao, String username, String defaultPlain) {
        dao.findByUsername(username).ifPresent(u -> {
            if (!PasswordUtil.looksLikeValidHash(u.getPasswordHash())) {
                String realHash = PasswordUtil.hash(defaultPlain);
                dao.updatePasswordHash(u.getId(), realHash);
                System.out.println("[bootstrap] Rewrote placeholder hash for '"
                        + username + "'. Default password: " + defaultPlain);
            }
        });
    }
}
