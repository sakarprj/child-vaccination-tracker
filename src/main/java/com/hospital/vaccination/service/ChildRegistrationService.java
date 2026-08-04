package com.hospital.vaccination.service;

import com.hospital.vaccination.config.DBConnection;
import com.hospital.vaccination.dao.ChildDAO;
import com.hospital.vaccination.dao.VaccinationRecordDAO;
import com.hospital.vaccination.model.Child;
import com.hospital.vaccination.model.VaccinationRecord;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Orchestrates a full "register a new child" action:
 *   1. Insert the child row
 *   2. Generate the full vaccination schedule
 *   3. Insert all vaccination_records rows
 * All inside a single transaction — if step 3 fails, step 1 is rolled back.
 */
public class ChildRegistrationService {

    private final ChildDAO              childDAO      = new ChildDAO();
    private final VaccinationRecordDAO  recordDAO     = new VaccinationRecordDAO();
    private final ScheduleGenerator     scheduleGen   = new ScheduleGenerator();

    /** Result bundle returned to the UI for the confirmation dialog. */
    public record Result(Child child, List<VaccinationRecord> records) { }

    public Result register(Child child) {
        Connection c = null;
        try {
            c = DBConnection.get();
            c.setAutoCommit(false);

            // 1. child row
            childDAO.insert(c, child);

            // 2. schedule
            List<VaccinationRecord> records =
                    scheduleGen.generateFor(child.getId(), child.getDateOfBirth());

            // 3. records
            recordDAO.batchInsert(c, records);

            c.commit();
            return new Result(child, records);

        } catch (SQLException e) {
            rollbackQuietly(c);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(c);
        }
    }

    private void rollbackQuietly(Connection c) {
        if (c == null) return;
        try { c.rollback(); } catch (SQLException ignored) { }
    }

    private void closeQuietly(Connection c) {
        if (c == null) return;
        try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) { }
    }
}