package com.hospital.vaccination.model;

import java.time.LocalDateTime;

/**
 * In-memory representation of a row in the <code>users</code> table.
 * Used for both Admin and Nurse accounts — the {@link Role} field
 * distinguishes them.
 */
public class User {

    public enum Role { ADMIN, NURSE }

    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private String fullName;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;   // updated whenever the user does anything in the app

    public User() { }

    public User(int id, String username, String passwordHash, Role role,
                String fullName, boolean active,
                LocalDateTime createdAt, LocalDateTime lastActiveAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.active = active;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    /**
     * Convenience presence classification based on {@link #lastActiveAt}.
     * <ul>
     *   <li>ONLINE   — activity in the last 10 minutes</li>
     *   <li>IDLE     — activity in the last 10–60 minutes</li>
     *   <li>OFFLINE  — never active, or over 60 minutes ago</li>
     * </ul>
     */
    public enum Presence { ONLINE, IDLE, OFFLINE }

    public Presence presence() {
        if (lastActiveAt == null) return Presence.OFFLINE;
        long minutes = java.time.Duration.between(lastActiveAt, LocalDateTime.now()).toMinutes();
        if (minutes < 10)  return Presence.ONLINE;
        if (minutes < 60)  return Presence.IDLE;
        return Presence.OFFLINE;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + ", " + role + ")";
    }
}
