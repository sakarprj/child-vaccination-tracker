package com.hospital.vaccination.service;

import com.hospital.vaccination.dao.ReminderLogDAO;
import com.hospital.vaccination.dao.VaccinationRecordDAO;
import com.hospital.vaccination.dao.VaccinationRecordDAO.ReminderCandidate;
import com.hospital.vaccination.util.DateUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The automated SMS reminder scheduler — the "brain" of the reminder
 * subsystem.
 *
 * <h3>Rules (from the project spec)</h3>
 * <ul>
 *   <li><b>Upcoming reminder:</b> 2–3 days before due date, send one SMS.
 *       Implemented as "any PENDING dose due between today+2 and today+3
 *       that hasn't received a reminder yet".</li>
 *   <li><b>Overdue follow-ups:</b> once a day for up to 5 days after
 *       the due date if the child hasn't shown up.</li>
 *   <li><b>Auto-mark MISSED:</b> after 5 follow-ups have been sent,
 *       stop reminding and flip the record's status to MISSED.</li>
 *   <li><b>Every SMS is logged</b> to <code>reminder_logs</code>.</li>
 * </ul>
 *
 * <h3>How it runs</h3>
 * The job runs once at application startup, then every 24 hours while
 * the app is open. It's idempotent — running it multiple times in the
 * same day won't send duplicate SMS, because the "upcoming" check
 * requires reminder_count == 0 and the "overdue" check advances the
 * count each time.
 */
public class ReminderJob {

    /** How many overdue days we tolerate (=how many follow-ups we send). */
    public static final int MAX_OVERDUE_DAYS = 5;
    /** Total reminder cap per record (1 upcoming + 5 overdue = 6). */
    public static final int MAX_REMINDERS    = 6;

    private final VaccinationRecordDAO recordDAO   = new VaccinationRecordDAO();
    private final ReminderLogDAO       logDAO      = new ReminderLogDAO();
    private final SmsService           sms;

    private ScheduledExecutorService scheduler;

    public ReminderJob(SmsService sms) {
        this.sms = sms;
    }

    // ---------------------------------------------------------- scheduling

    /**
     * Start the daemon: run one pass immediately (5-second delay so the
     * UI has time to appear first), then repeat every 24 hours.
     */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reminder-job");
            t.setDaemon(true);   // don't block JVM shutdown
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::safeRun, 5, 24 * 60 * 60, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    /** Run once, catching any exception so the scheduler thread survives. */
    private void safeRun() {
        try {
            runOnce(LocalDate.now());
        } catch (Exception ex) {
            System.err.println("[reminder-job] pass failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ------------------------------------------------------------ core

    /**
     * One pass of the reminder logic. Extracted from the scheduler so it
     * can also be triggered manually (e.g. from an Admin "Run reminders
     * now" button in a later step, or from a unit test).
     *
     * @return small summary counts for logging / display
     */
    public Summary runOnce(LocalDate today) {
        System.out.println("[reminder-job] pass starting for " +
                DateUtil.display(today));

        int upcoming = sendUpcoming(today);
        int overdue  = sendOverdueFollowups(today);
        int missed   = recordDAO.autoMarkMissed(today, MAX_OVERDUE_DAYS, MAX_REMINDERS);

        Summary s = new Summary(upcoming, overdue, missed);
        System.out.println("[reminder-job] done — " + s);
        return s;
    }

    // ---- Rule 1: "upcoming in 2-3 days" -----------------------------------

    private int sendUpcoming(LocalDate today) {
        LocalDate from = today.plusDays(2);
        LocalDate to   = today.plusDays(3);

        List<ReminderCandidate> due = recordDAO.findPendingDueBetween(from, to);
        int sent = 0;
        for (ReminderCandidate rc : due) {
            // Only send the "upcoming" SMS once per record
            if (rc.record().getReminderCount() != 0) continue;

            String msg = String.format(
                    "Reminder: %s's vaccine %s is due on %s. " +
                            "Please visit the hospital. Bring the vaccination card.",
                    rc.childName(),
                    rc.record().getVaccineName(),
                    DateUtil.display(rc.record().getDueDate()));

            if (dispatch(rc, msg)) sent++;
        }
        return sent;
    }

    // ---- Rule 2: "overdue: once a day for 5 days" -------------------------

    private int sendOverdueFollowups(LocalDate today) {
        List<ReminderCandidate> overdue =
                recordDAO.findOverduePending(today, MAX_REMINDERS);
        int sent = 0;
        for (ReminderCandidate rc : overdue) {
            long daysOverdue = today.toEpochDay() - rc.record().getDueDate().toEpochDay();
            if (daysOverdue < 1 || daysOverdue > MAX_OVERDUE_DAYS) continue;

            // Rate-limit: only one SMS per day per record.
            // Cheap heuristic: reminder_count equals (# followups already sent) + 1 upcoming.
            // Expected count today = 1 (upcoming) + daysOverdue. Skip if we've hit it.
            int expected = 1 + (int) daysOverdue;
            if (rc.record().getReminderCount() >= expected) continue;

            String msg = String.format(
                    "URGENT: %s's vaccine %s was due on %s (%d day%s ago). " +
                            "Please visit the hospital as soon as possible.",
                    rc.childName(),
                    rc.record().getVaccineName(),
                    DateUtil.display(rc.record().getDueDate()),
                    daysOverdue,
                    daysOverdue == 1 ? "" : "s");

            if (dispatch(rc, msg)) sent++;
        }
        return sent;
    }

    // ---- dispatch: send + log + bump counter, all together ----------------

    private boolean dispatch(ReminderCandidate rc, String message) {
        boolean ok;
        try {
            ok = sms.send(rc.parentPhone(), message);
        } catch (Exception ex) {
            System.err.println("[reminder-job] SMS failed for record #" +
                    rc.record().getId() + ": " + ex.getMessage());
            return false;
        }
        if (!ok) return false;

        // Log & increment count. If either fails we still report success,
        // because the SMS did go out — we just note the accounting error.
        try {
            logDAO.insert(rc.record().getId(), rc.parentPhone(), message);
            recordDAO.incrementReminderCount(rc.record().getId());
        } catch (Exception ex) {
            System.err.println("[reminder-job] log/count failed for record #" +
                    rc.record().getId() + ": " + ex.getMessage());
        }
        return true;
    }

    // ---- summary value object --------------------------------------------

    public record Summary(int upcomingSent, int overdueSent, int newlyMissed) {
        @Override public String toString() {
            return upcomingSent + " upcoming, " +
                    overdueSent  + " overdue, " +
                    newlyMissed  + " newly-missed";
        }
    }
}
