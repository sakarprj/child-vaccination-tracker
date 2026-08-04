package com.hospital.vaccination.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Real SMS gateway backed by <a href="https://sparrowsms.com">Sparrow SMS</a>
 * — Nepal's most widely-used SMS aggregator.
 *
 * <h3>API contract</h3>
 * <ul>
 *   <li><b>Endpoint:</b> {@code http://api.sparrowsms.com/v2/sms/}</li>
 *   <li><b>Method:</b> POST, {@code application/x-www-form-urlencoded}</li>
 *   <li><b>Params:</b>
 *     <ul>
 *       <li>{@code token} — API token from your Sparrow dashboard</li>
 *       <li>{@code from}  — Sender identity (a "sender ID" string Sparrow assigns)</li>
 *       <li>{@code to}    — 10-digit mobile number (or comma-separated list)</li>
 *       <li>{@code text}  — Message body</li>
 *     </ul>
 *   </li>
 *   <li><b>Success response:</b> HTTP 200 + JSON with {@code response_code: 200}</li>
 * </ul>
 *
 * <p>Uses Java 17's built-in {@link HttpClient} — no third-party HTTP
 * library needed, so nothing extra in {@code pom.xml}.
 *
 * <p>Get your credentials at <a href="https://sparrowsms.com">sparrowsms.com</a>.
 */
public class SparrowSmsService implements SmsService {

    private static final String ENDPOINT = "http://api.sparrowsms.com/v2/sms/";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final String token;
    private final String from;
    private final HttpClient client;

    /**
     * @param token API token from your Sparrow dashboard
     * @param from  Sender identity string (e.g. "Demo" or a registered
     *              sender ID like "MyClinic")
     */
    public SparrowSmsService(String token, String from) {
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Sparrow token is required");
        if (from == null || from.isBlank())
            throw new IllegalArgumentException("Sparrow 'from' identity is required");
        this.token = token;
        this.from  = from;
        this.client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    @Override
    public boolean send(String phone, String message) {
        String toNumber = normalize(phone);
        if (toNumber == null) {
            System.err.println("[sparrow] refused: invalid phone number '" + phone + "'");
            return false;
        }

        String body = "token=" + enc(token)
                + "&from="  + enc(from)
                + "&to="    + enc(toNumber)
                + "&text="  + enc(message);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp =
                    client.send(req, HttpResponse.BodyHandlers.ofString());

            boolean ok = resp.statusCode() == 200
                    && resp.body() != null
                    && resp.body().contains("\"response_code\": 200");

            if (!ok) {
                System.err.println("[sparrow] SMS to " + toNumber +
                        " failed: HTTP " + resp.statusCode() +
                        "  body=" + trim(resp.body(), 200));
            }
            return ok;

        } catch (Exception ex) {
            System.err.println("[sparrow] SMS to " + toNumber +
                    " threw: " + ex.getMessage());
            return false;
        }
    }

    // ---- helpers ------------------------------------------------------------

    /**
     * Sparrow expects a 10-digit Nepali mobile number (no +977 prefix, no
     * spaces, no dashes). Strip anything else and validate length.
     */
    private static String normalize(String phone) {
        if (phone == null) return null;
        // Drop everything except digits
        String digits = phone.replaceAll("\\D", "");
        // Strip 977 country code if present
        if (digits.startsWith("977") && digits.length() == 13) {
            digits = digits.substring(3);
        }
        return digits.length() == 10 ? digits : null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
