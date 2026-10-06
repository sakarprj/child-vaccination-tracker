package com.hospital.vaccination.dao;

import com.hospital.vaccination.config.DBConnection;
import com.hospital.vaccination.model.VaccinationRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the vaccination_records table. */
public class VaccinationRecordDAO {

    /**
     * Insert many records in one batch, on the given connection
     * (so the whole child-registration action is one transaction).
     */
    public void batchInsert(Connection c, List<VaccinationRecord> records) throws SQLException {
        String sql = "INSERT INTO vaccination_records " +
                "(child_id, vaccine_name, due_date, status, reminder_count) " +
                "VALUES (?, ?, ?, ?, 0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (VaccinationRecord r : records) {
                ps.setInt   (1, r.getChildId());
                ps.setString(2, r.getVaccineName());
                ps.setDate  (3, Date.valueOf(r.getDueDate()));
                ps.setString(4, r.getStatus().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<VaccinationRecord> findByChild(int childId) {
        String sql = "SELECT * FROM vaccination_records WHERE child_id = ? ORDER BY due_date";
        return query(sql, ps -> ps.setInt(1, childId));
    }

    /** ALL records due on a given date, regardless of status (used by the checklist). */
    public List<VaccinationRecord> findDueOn(LocalDate date) {
        String sql = "SELECT * FROM vaccination_records " +
                "WHERE due_date = ? " +
                "ORDER BY status, id";
        return query(sql, ps -> ps.setDate(1, Date.valueOf(date)));
    }

    /**
     * PENDING records whose due date falls within the given inclusive
     * window (e.g. today+2 to today+3 for "upcoming" SMS reminders).
     */
    public List<ReminderCandidate> findPendingDueBetween(LocalDate from, LocalDate to) {
        String sql =
                "SELECT vr.id, vr.child_id, vr.vaccine_name, vr.due_date, " +
                        "       vr.status, vr.completed_date, vr.reminder_count, " +
                        "       c.name AS child_name, c.parent_name, c.parent_phone " +
                        "FROM vaccination_records vr " +
                        "JOIN children c ON c.id = vr.child_id " +
                        "WHERE vr.status = 'PENDING' " +
                        "  AND vr.due_date BETWEEN ? AND ? " +
                        "ORDER BY vr.due_date, vr.id";
        return joinQuery(sql, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    /**
     * PENDING records that are overdue (due date already passed) and
     * still have room for more follow-up reminders (count &lt; maxCount).
     */
    public List<ReminderCandidate> findOverduePending(LocalDate asOf, int maxCount) {
        String sql =
                "SELECT vr.id, vr.child_id, vr.vaccine_name, vr.due_date, " +
                        "       vr.status, vr.completed_date, vr.reminder_count, " +
                        "       c.name AS child_name, c.parent_name, c.parent_phone " +
                        "FROM vaccination_records vr " +
                        "JOIN children c ON c.id = vr.child_id " +
                        "WHERE vr.status = 'PENDING' " +
                        "  AND vr.due_date < ? " +
                        "  AND vr.reminder_count < ? " +
                        "ORDER BY vr.due_date, vr.id";
        return joinQuery(sql, ps -> {
            ps.setDate(1, Date.valueOf(asOf));
            ps.setInt (2, maxCount);
        });
    }

    /** Increment reminder_count by 1 for the given record. */
    public void incrementReminderCount(int recordId) {
        String sql = "UPDATE vaccination_records " +
                "SET reminder_count = reminder_count + 1 WHERE id = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("incrementReminderCount failed", e);
        }
    }

    /**
     * Bulk auto-mark: any PENDING record whose due date is more than
     * maxOverdueDays behind and whose reminder_count has reached
     * maxReminders → status = MISSED. Returns how many rows changed.
     */
    public int autoMarkMissed(LocalDate asOf, int maxOverdueDays, int maxReminders) {
        String sql = "UPDATE vaccination_records " +
                "SET status = 'MISSED' " +
                "WHERE status = 'PENDING' " +
                "  AND due_date < ? " +
                "  AND reminder_count >= ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(asOf.minusDays(maxOverdueDays)));
            ps.setInt (2, maxReminders);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("autoMarkMissed failed", e);
        }
    }

    /**
     * Richer join query for the checklist: pulls child name, DOB,
     * parent phone alongside each due record. One trip to the DB per refresh.
     */
    public List<ChecklistRow> findChecklistFor(LocalDate date) {
        String sql =
                "SELECT vr.id, vr.child_id, vr.vaccine_name, vr.due_date, " +
                        "       vr.status, vr.completed_date, vr.reminder_count, " +
                        "       c.name AS child_name, c.date_of_birth AS dob, " +
                        "       c.parent_phone " +
                        "FROM vaccination_records vr " +
                        "JOIN children c ON c.id = vr.child_id " +
                        "WHERE vr.due_date = ? " +
                        "ORDER BY vr.status, c.name, vr.vaccine_name";
        List<ChecklistRow> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date cd = rs.getDate("completed_date");
                    VaccinationRecord r = new VaccinationRecord(
                            rs.getInt("id"),
                            rs.getInt("child_id"),
                            rs.getString("vaccine_name"),
                            rs.getDate("due_date").toLocalDate(),
                            VaccinationRecord.Status.valueOf(rs.getString("status")),
                            cd == null ? null : cd.toLocalDate(),
                            rs.getInt("reminder_count"));
                    out.add(new ChecklistRow(
                            r,
                            rs.getString("child_name"),
                            rs.getDate("dob").toLocalDate(),
                            rs.getString("parent_phone")));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findChecklistFor failed", e);
        }
    }

    /**
     * All future PENDING doses, starting tomorrow. Results are always ordered
     * by nearest due date first so staff can plan the upcoming workload.
     */
    public List<ChecklistRow> findUpcomingChecklist(LocalDate today) {
        String sql =
                "SELECT vr.id, vr.child_id, vr.vaccine_name, vr.due_date, " +
                        "       vr.status, vr.completed_date, vr.reminder_count, " +
                        "       c.name AS child_name, c.date_of_birth AS dob, " +
                        "       c.parent_phone " +
                        "FROM vaccination_records vr " +
                        "JOIN children c ON c.id = vr.child_id " +
                        "WHERE vr.status = 'PENDING' " +
                        "  AND vr.due_date > ? " +
                        "ORDER BY vr.due_date ASC, c.name ASC, vr.id ASC";

        List<ChecklistRow> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date completed = rs.getDate("completed_date");
                    VaccinationRecord record = new VaccinationRecord(
                            rs.getInt("id"),
                            rs.getInt("child_id"),
                            rs.getString("vaccine_name"),
                            rs.getDate("due_date").toLocalDate(),
                            VaccinationRecord.Status.valueOf(rs.getString("status")),
                            completed == null ? null : completed.toLocalDate(),
                            rs.getInt("reminder_count"));

                    out.add(new ChecklistRow(
                            record,
                            rs.getString("child_name"),
                            rs.getDate("dob").toLocalDate(),
                            rs.getString("parent_phone")));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findUpcomingChecklist failed", e);
        }
    }

    public void markCompleted(int recordId, LocalDate when) {
        String sql = "UPDATE vaccination_records " +
                "SET status = 'COMPLETED', completed_date = ? WHERE id = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(when));
            ps.setInt (2, recordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("markCompleted failed", e);
        }
    }

    public void markPending(int recordId) {
        String sql = "UPDATE vaccination_records " +
                "SET status = 'PENDING', completed_date = NULL WHERE id = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("markPending failed", e);
        }
    }

    // ---------------- helpers / value objects ----------------

    /**
     * Value object combining a vaccination record with its child's
     * basic info — used by the checklist UI.
     */
    public record ChecklistRow(VaccinationRecord record,
                               String childName,
                               LocalDate childDob,
                               String parentPhone) { }

    /**
     * Value object for the reminder job — everything needed to build
     * and send one SMS, in one row.
     */
    public record ReminderCandidate(VaccinationRecord record,
                                    String childName,
                                    String parentName,
                                    String parentPhone) { }

    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<VaccinationRecord> query(String sql, Binder binder) {
        List<VaccinationRecord> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("query failed: " + sql, e);
        }
    }

    private List<ReminderCandidate> joinQuery(String sql, Binder binder) {
        List<ReminderCandidate> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date cd = rs.getDate("completed_date");
                    VaccinationRecord r = new VaccinationRecord(
                            rs.getInt("id"),
                            rs.getInt("child_id"),
                            rs.getString("vaccine_name"),
                            rs.getDate("due_date").toLocalDate(),
                            VaccinationRecord.Status.valueOf(rs.getString("status")),
                            cd == null ? null : cd.toLocalDate(),
                            rs.getInt("reminder_count"));
                    out.add(new ReminderCandidate(
                            r,
                            rs.getString("child_name"),
                            rs.getString("parent_name"),
                            rs.getString("parent_phone")));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("joinQuery failed", e);
        }
    }

    private VaccinationRecord map(ResultSet rs) throws SQLException {
        Date completed = rs.getDate("completed_date");
        return new VaccinationRecord(
                rs.getInt("id"),
                rs.getInt("child_id"),
                rs.getString("vaccine_name"),
                rs.getDate("due_date").toLocalDate(),
                VaccinationRecord.Status.valueOf(rs.getString("status")),
                completed == null ? null : completed.toLocalDate(),
                rs.getInt("reminder_count")
        );
    }
}
