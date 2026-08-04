package com.hospital.vaccination.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Development/demo SMS "gateway" that just prints each message to
 * the IntelliJ Run console. Perfect for testing the reminder logic
 * without needing a real SMS vendor or spending money on messages.
 *
 * <p>Every call also returns true, simulating a successful delivery.
 * The actual persistent log lives in the <code>reminder_logs</code>
 * table (written by {@link ReminderJob}).
 */
public class ConsoleSmsService implements SmsService {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean send(String phone, String message) {
        System.out.println(
                "[SMS " + TS.format(LocalDateTime.now()) + "] " +
                        "→ " + phone + "\n    " + message);
        return true;
    }
}
