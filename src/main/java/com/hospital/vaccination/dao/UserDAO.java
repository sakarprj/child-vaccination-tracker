package com.hospital.vaccination.dao;

import com.hospital.vaccination.config.DBConnection;
import com.hospital.vaccination.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the <code>users</code> table.
 * Called by AuthService (for login) and by the Admin panel
 * (for nurse management).
 */
public class UserDAO {

    // ---------- read ----------

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND active = TRUE";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed", e);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY role, username";
        List<User> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
    }

    public List<User> findAllNurses() {
        String sql = "SELECT * FROM users WHERE role = 'NURSE' ORDER BY username";
        List<User> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAllNurses failed", e);
        }
    }

    // ---------- write ----------

    public int insert(User u) {
        String sql = "INSERT INTO users (username, password, role, full_name, active) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getRole().name());
            ps.setString(4, u.getFullName());
            ps.setBoolean(5, u.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("insert user failed", e);
        }
    }

    public void updatePasswordHash(int userId, String newHash) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updatePasswordHash failed", e);
        }
    }

    public void deactivate(int userId) {
        String sql = "UPDATE users SET active = FALSE WHERE id = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deactivate failed", e);
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("usernameExists failed", e);
        }
    }

    /**
     * Mark a user as active RIGHT NOW. Called on login and on every UI
     * interaction (tab switch, refresh, etc.). Silently swallows errors
     * because a failed touch shouldn't break the app — the presence
     * indicator is a nice-to-have, not critical.
     */
    public void touch(int userId) {
        String sql = "UPDATE users SET last_active_at = NOW() WHERE id = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) { /* non-fatal */ }
    }

    // ---------- helper ----------

    private User map(ResultSet rs) throws SQLException {
        Timestamp ts   = rs.getTimestamp("created_at");
        // last_active_at may not exist if migration hasn't run yet — check safely
        LocalDateTime last = null;
        try {
            Timestamp ts2 = rs.getTimestamp("last_active_at");
            if (ts2 != null) last = ts2.toLocalDateTime();
        } catch (SQLException ignored) {
            // column missing (old DB schema) — treat as null
        }
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                User.Role.valueOf(rs.getString("role")),
                rs.getString("full_name"),
                rs.getBoolean("active"),
                ts == null ? null : ts.toLocalDateTime(),
                last
        );
    }
}
