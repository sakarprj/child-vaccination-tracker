package com.hospital.vaccination.model;

import java.time.LocalDate;

/** One dose scheduled (or given) for one child. */
public class VaccinationRecord {

    public enum Status { PENDING, COMPLETED, MISSED }

    private int id;
    private int childId;
    private String vaccineName;    // e.g. "Penta-1", "MR-2", "BCG"
    private LocalDate dueDate;
    private Status status;
    private LocalDate completedDate;  // null until given
    private int reminderCount;

    public VaccinationRecord() { }

    public VaccinationRecord(int id, int childId, String vaccineName,
                             LocalDate dueDate, Status status,
                             LocalDate completedDate, int reminderCount) {
        this.id = id;
        this.childId = childId;
        this.vaccineName = vaccineName;
        this.dueDate = dueDate;
        this.status = status;
        this.completedDate = completedDate;
        this.reminderCount = reminderCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChildId() { return childId; }
    public void setChildId(int childId) { this.childId = childId; }

    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }

    public int getReminderCount() { return reminderCount; }
    public void setReminderCount(int reminderCount) { this.reminderCount = reminderCount; }
}