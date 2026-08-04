package com.hospital.vaccination.dao;

import com.hospital.vaccination.config.DBConnection;
import com.hospital.vaccination.model.ReminderLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** SQL for the reminder_logs audit table. */
public class ReminderLogDAO {

    /** Insert a new log row. Sent timestamp defaults to NOW() in the DB. */
    public int insert(int recordId, String phone, String message) {
        String sql = "INSERT INTO reminder_logs (record_id, phone_number, message) " +
                "VALUES (?, ?, ?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, recordId);
            ps.setString(2, phone);
            ps.setString(3, message);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("insert reminder log failed", e);
        }
    }

    /** All logs for a given vaccination_record (used by Admin history views). */
    public List<ReminderLog> findByRecord(int recordId) {
        String sql = "SELECT * FROM reminder_logs WHERE record_id = ? ORDER BY sent_at";
        List<ReminderLog> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("sent_at");
                    out.add(new ReminderLog(
                            rs.getInt("id"),
                            rs.getInt("record_id"),
                            rs.getString("phone_number"),
                            rs.getString("message"),
                            ts == null ? null : ts.toLocalDateTime()));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findByRecord failed", e);
        }
    }

    /** Total SMS count across the whole system (useful for admin stats). */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM reminder_logs";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("countAll failed", e);
        }
    }
}
