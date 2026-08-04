package com.hospital.vaccination.config;

import com.hospital.vaccination.service.ConsoleSmsService;
import com.hospital.vaccination.service.SmsService;
import com.hospital.vaccination.service.SparrowSmsService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Builds an {@link SmsService} at startup based on configuration.
 *
 * <h3>Where credentials come from — in order of priority</h3>
 * <ol>
 *   <li><b>Environment variables</b> (best for production):
 *     <ul>
 *       <li>{@code SPARROW_SMS_TOKEN}</li>
 *       <li>{@code SPARROW_SMS_FROM}</li>
 *     </ul>
 *   </li>
 *   <li><b>{@code sms.properties}</b> on the classpath
 *     ({@code src/main/resources/sms.properties}):
 *     <pre>
 *     sparrow.token=abcXYZ12345
 *     sparrow.from=MyClinic
 *     </pre>
 *   </li>
 * </ol>
 *
 * <h3>Fallback</h3>
 * If neither source provides both a token and a "from" identity, we
 * silently return {@link ConsoleSmsService} instead — so the app still
 * runs during development / classroom demo, and every "sent" SMS gets
 * printed to the Run console. A banner is logged at startup so nobody
 * is confused about which mode is active.
 */
public final class SmsConfig {

    private SmsConfig() { }

    public static SmsService build() {
        String token = firstNonBlank(
                System.getenv("SPARROW_SMS_TOKEN"),
                loadProperty("sparrow.token"));

        String from  = firstNonBlank(
                System.getenv("SPARROW_SMS_FROM"),
                loadProperty("sparrow.from"));

        if (isBlank(token) || isBlank(from)) {
            banner(
                    "SMS mode: CONSOLE (simulator)",
                    "No Sparrow credentials found. Every 'sent' SMS will be",
                    "printed to this console instead. To enable real SMS,",
                    "either:",
                    "  • set SPARROW_SMS_TOKEN and SPARROW_SMS_FROM env vars, or",
                    "  • create src/main/resources/sms.properties from the",
                    "    provided sms.properties.example");
            return new ConsoleSmsService();
        }

        banner(
                "SMS mode: SPARROW SMS (live)",
                "Sender identity: " + from,
                "Real SMS messages WILL be sent to parents.");
        return new SparrowSmsService(token, from);
    }

    // ---- helpers ------------------------------------------------------------

    /** Read a property from src/main/resources/sms.properties. */
    private static String loadProperty(String key) {
        try (InputStream in = SmsConfig.class
                .getClassLoader()
                .getResourceAsStream("sms.properties")) {
            if (in == null) return null;
            Properties p = new Properties();
            p.load(in);
            return p.getProperty(key);
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String firstNonBlank(String... options) {
        for (String s : options) if (!isBlank(s)) return s;
        return null;
    }

    private static void banner(String... lines) {
        String bar = "=".repeat(60);
        System.out.println("\n" + bar);
        for (String line : lines) System.out.println("  " + line);
        System.out.println(bar + "\n");
    }
}
