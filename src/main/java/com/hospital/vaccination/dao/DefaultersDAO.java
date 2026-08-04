package com.hospital.vaccination.dao;

import com.hospital.vaccination.config.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated queries for the "Defaulters Report" — children with one
 * or more MISSED doses that hospital staff need to physically follow
 * up with (home visit, community health worker, etc.).
 */
public class DefaultersDAO {

    /**
     * One row per defaulting child, with a count of how many doses
     * they've missed and a comma-separated list of the vaccine names.
     */
    public List<DefaulterRow> findAll() {
        String sql =
                "SELECT c.id, c.name, c.date_of_birth, c.parent_name, " +
                        "       c.parent_phone, c.address, " +
                        "       COUNT(vr.id) AS missed_count, " +
                        "       GROUP_CONCAT(vr.vaccine_name ORDER BY vr.due_date " +
                        "                    SEPARATOR ', ') AS missed_vaccines " +
                        "FROM children c " +
                        "JOIN vaccination_records vr ON vr.child_id = c.id " +
                        "WHERE vr.status = 'MISSED' " +
                        "GROUP BY c.id " +
                        "ORDER BY missed_count DESC, c.name";

        List<DefaulterRow> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new DefaulterRow(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDate("date_of_birth").toLocalDate(),
                        rs.getString("parent_name"),
                        rs.getString("parent_phone"),
                        rs.getString("address"),
                        rs.getInt("missed_count"),
                        rs.getString("missed_vaccines")));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAll defaulters failed", e);
        }
    }

    /** Value object for one row in the defaulters report. */
    public record DefaulterRow(int childId,
                               String childName,
                               LocalDate childDob,
                               String parentName,
                               String parentPhone,
                               String address,
                               int missedCount,
                               String missedVaccines) { }
}
