package com.hospital.vaccination.service;

import com.hospital.vaccination.dao.UserDAO;
import com.hospital.vaccination.model.User;
import com.hospital.vaccination.util.PasswordUtil;

import java.util.Optional;

/**
 * Login / logout business logic.
 * Also exposes {@link #touch()} so the UI can update the current user's
 * last-active timestamp whenever they interact with the app.
 */
public class AuthService {

    private static final UserDAO userDAO = new UserDAO();

    /** Currently logged-in user; null when nobody is signed in. */
    private static User currentUser;

    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> maybe = userDAO.findByUsername(username.trim());
        if (maybe.isEmpty()) return Optional.empty();

        User u = maybe.get();
        if (!PasswordUtil.verify(password, u.getPasswordHash())) {
            return Optional.empty();
        }

        currentUser = u;
        userDAO.touch(u.getId());   // record login activity
        return Optional.of(u);
    }

    public static User getCurrentUser() { return currentUser; }

    public static void logout() { currentUser = null; }

    /**
     * Update the current user's last-active-at timestamp. Safe to call
     * frequently — the UI pings this on tab switches, refreshes, etc.
     * No-op if nobody is logged in.
     */
    public static void touch() {
        if (currentUser != null) userDAO.touch(currentUser.getId());
    }
}
