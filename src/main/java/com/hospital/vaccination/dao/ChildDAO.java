package com.hospital.vaccination.dao;

import com.hospital.vaccination.config.DBConnection;
import com.hospital.vaccination.model.Child;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All SQL for the children table. */
public class ChildDAO {

    /**
     * Insert using an existing connection so we can share a transaction
     * with VaccinationRecordDAO.batchInsert().
     */
    public int insert(Connection c, Child child) throws SQLException {
        String sql = "INSERT INTO children " +
                "(name, date_of_birth, gender, parent_name, parent_phone, address, registered_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, child.getName());
            ps.setDate  (2, Date.valueOf(child.getDateOfBirth()));
            ps.setString(3, child.getGender().name());
            ps.setString(4, child.getParentName());
            ps.setString(5, child.getParentPhone());
            ps.setString(6, child.getAddress());
            ps.setInt   (7, child.getRegisteredBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    child.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("No generated id returned when inserting child");
    }

    public Optional<Child> findById(int id) {
        String sql = "SELECT * FROM children WHERE id = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    public List<Child> findAll() {
        String sql = "SELECT * FROM children ORDER BY registered_at DESC";
        List<Child> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
    }

    /**
     * Richer query for the Admin "All Children" tab — includes the
     * nurse's full name (JOIN users), and supports an optional
     * case-insensitive filter that matches either child name or
     * parent phone (LIKE).
     *
     * @param filter partial name/phone; null or blank returns everyone
     */
    public List<ChildRow> findAllForAdmin(String filter) {
        boolean hasFilter = filter != null && !filter.isBlank();
        String sql =
                "SELECT c.*, u.full_name AS nurse_name " +
                        "FROM children c " +
                        "LEFT JOIN users u ON u.id = c.registered_by " +
                        (hasFilter
                                ? "WHERE c.name LIKE ? OR c.parent_phone LIKE ? "
                                : "") +
                        "ORDER BY c.registered_at DESC";

        List<ChildRow> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (hasFilter) {
                String like = "%" + filter.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAllForAdmin failed", e);
        }
    }

    // ---------------- helpers / value objects ----------------

    /** Child + registering nurse's name — for the admin "All Children" table. */
    public record ChildRow(Child child, String nurseName) { }

    private Child map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("registered_at");
        return new Child(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDate("date_of_birth").toLocalDate(),
                Child.Gender.valueOf(rs.getString("gender")),
                rs.getString("parent_name"),
                rs.getString("parent_phone"),
                rs.getString("address"),
                rs.getInt("registered_by"),
                ts == null ? null : ts.toLocalDateTime()
        );
    }

    private ChildRow mapRow(ResultSet rs) throws SQLException {
        return new ChildRow(map(rs), rs.getString("nurse_name"));
    }
}
