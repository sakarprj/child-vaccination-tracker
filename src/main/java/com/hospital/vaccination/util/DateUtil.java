package com.hospital.vaccination.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Tiny date helpers used across UI + services. */
public final class DateUtil {

    public static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy");   // 12 Jul 2026
    public static final DateTimeFormatter INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");    // 2026-07-12

    private DateUtil() { }

    public static String display(LocalDate d) {
        return d == null ? "" : DISPLAY.format(d);
    }

    /** Parses YYYY-MM-DD; returns null if blank or malformed. */
    public static LocalDate parse(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), INPUT);
        } catch (Exception e) {
            return null;
        }
    }
}