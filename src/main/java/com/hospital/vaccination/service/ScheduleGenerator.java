package com.hospital.vaccination.service;

import com.hospital.vaccination.model.VaccinationRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the full vaccination schedule for a child based on
 * date of birth, per the Nepal National Immunization Schedule (2024).
 *
 * The schedule is hardcoded as a small table (see SCHEDULE below) so
 * you can adjust vaccines or ages in one place if guidelines change.
 */
public class ScheduleGenerator {

    /** One entry = one vaccine dose at a specific age offset from DOB. */
    private record Dose(String vaccineName, int daysAfterBirth) { }

    /** Handy constants so the table below reads naturally. */
    private static final int WEEK = 7;
    private static final int MONTH = 30;      // approximation is fine for a schedule

    /**
     * Nepal NIP schedule as of 2024. Order = order they'll appear on
     * the printed card. Total: 17 doses.
     */
    private static final List<Dose> SCHEDULE = List.of(
            // At birth
            new Dose("BCG",     0),

            // 6 weeks
            new Dose("Penta-1", 6 * WEEK),
            new Dose("OPV-1",   6 * WEEK),
            new Dose("PCV-1",   6 * WEEK),
            new Dose("Rota-1",  6 * WEEK),

            // 10 weeks
            new Dose("Penta-2", 10 * WEEK),
            new Dose("OPV-2",   10 * WEEK),
            new Dose("Rota-2",  10 * WEEK),

            // 14 weeks
            new Dose("Penta-3", 14 * WEEK),
            new Dose("OPV-3",   14 * WEEK),
            new Dose("fIPV-1",  14 * WEEK),

            // 9 months
            new Dose("MR-1",    9 * MONTH),
            new Dose("PCV-2",   9 * MONTH),
            new Dose("fIPV-2",  9 * MONTH),

            // 12 months
            new Dose("JE",      12 * MONTH),

            // 15 months
            new Dose("MR-2",    15 * MONTH),
            new Dose("PCV-3",   15 * MONTH),
            new Dose("TCV",     15 * MONTH)   // Typhoid Conjugate Vaccine
    );

    /**
     * Build the full list of VaccinationRecord objects for a child.
     * childId is set on each record so the caller can insert them
     * straight away.
     */
    public List<VaccinationRecord> generateFor(int childId, LocalDate dob) {
        if (dob == null) throw new IllegalArgumentException("DOB is required");

        List<VaccinationRecord> out = new ArrayList<>(SCHEDULE.size());
        for (Dose d : SCHEDULE) {
            VaccinationRecord r = new VaccinationRecord();
            r.setChildId(childId);
            r.setVaccineName(d.vaccineName());
            r.setDueDate(dob.plusDays(d.daysAfterBirth()));
            r.setStatus(VaccinationRecord.Status.PENDING);
            r.setReminderCount(0);
            out.add(r);
        }
        return out;
    }

    /** How many doses this generator will produce. Useful in the UI summary. */
    public int totalDoses() { return SCHEDULE.size(); }
}