package com.hospital.vaccination.model;

import java.time.LocalDateTime;

/** One row in the reminder_logs table — an audit trail entry for one SMS. */
public class ReminderLog {

    private int id;
    private int recordId;          // FK -> vaccination_records.id
    private String phoneNumber;
    private String message;
    private LocalDateTime sentAt;

    public ReminderLog() { }

    public ReminderLog(int id, int recordId, String phoneNumber,
                       String message, LocalDateTime sentAt) {
        this.id = id;
        this.recordId = recordId;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.sentAt = sentAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
