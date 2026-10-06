package com.hospital.vaccination.service;

/**
 * SMS reminder abstraction used by ReminderJob.
 *
 * The current project uses ConsoleSmsService only.
 * It simulates SMS delivery by printing reminders
 * in the IntelliJ Run console.
 */
public interface SmsService {

    /**
     * Sends a simulated reminder message.
     *
     * @param phone   Parent or guardian phone number
     * @param message Reminder message content
     * @return true when the simulator accepts the reminder
     */
    boolean send(String phone, String message);
}