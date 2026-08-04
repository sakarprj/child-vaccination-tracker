package com.hospital.vaccination.service;

/**
 * Pluggable SMS gateway.
 *
 * <p>Any implementation just needs to answer: "given this phone number
 * and this message, send it." Return true on success, false on failure.
 *
 * <p>Ships with {@link ConsoleSmsService} for development/demo. In
 * production, drop in a real class (e.g. {@code TwilioSmsService},
 * {@code SparrowSmsService}) that hits the vendor's HTTP API — nothing
 * else in the app needs to change.
 */
public interface SmsService {

    /**
     * @param phone    E.164-ish phone number, e.g. "9841234567" or "+9779841234567"
     * @param message  the SMS body (kept &lt; 160 chars where possible)
     * @return         true if the gateway accepted it, false otherwise
     */
    boolean send(String phone, String message);
}
