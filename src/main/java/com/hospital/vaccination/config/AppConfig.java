package com.hospital.vaccination.config;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Application-wide branding / metadata.
 *
 * <p>Reads from <code>src/main/resources/app.properties</code> at startup.
 * Non-secret, safe to commit. Editable without recompiling.
 *
 * <p>Also caches the app logo (loaded from <code>images/logo.png</code>)
 * so every window can grab a scaled copy without re-reading the file.
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();
    private static Image LOGO;
    private static Image FALLBACK_LOGO;

    static {
        try (InputStream in = AppConfig.class
                .getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) PROPS.load(in);
        } catch (IOException e) {
            // Non-fatal — fall back to hard-coded defaults below.
        }
        // Try several common filenames so the app is forgiving of
        // whichever name the developer used when dropping the PNG in.
        for (String candidate : new String[]{
                "images/logo.png",
                "images/img.png",
                "logo.png",
                "img.png" }) {
            try (InputStream in = AppConfig.class
                    .getClassLoader().getResourceAsStream(candidate)) {
                if (in != null) {
                    LOGO = new ImageIcon(in.readAllBytes()).getImage();
                    break;
                }
            } catch (IOException ignored) { }
        }
    }

    private AppConfig() { }

    // ---- branding text ----------------------------------------------------

    public static String hospitalName() {
        return PROPS.getProperty("hospital.name", "Child Vaccination");
    }

    public static String hospitalTagline() {
        return PROPS.getProperty("hospital.tagline",
                "Nepal National Immunization Schedule");
    }

    public static String hospitalAddress() {
        return PROPS.getProperty("hospital.address", "").trim();
    }

    public static String appVersion() {
        return PROPS.getProperty("app.version", "1.0.0");
    }

    /** e.g. "Child Vaccination — Login" for a window title. */
    public static String windowTitle(String pageName) {
        return hospitalName() + " — " + pageName;
    }

    // ---- logo -------------------------------------------------------------

    /**
     * @param size desired icon width & height in pixels
     * @return the app logo scaled to {@code size}×{@code size},
     *         or a simple procedural fallback if the PNG couldn't load
     */
    public static ImageIcon logo(int size) {
        Image src = LOGO != null ? LOGO : fallbackLogo();
        Image scaled = src.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /** Raw image — pass to {@link JFrame#setIconImage(Image)}. */
    public static Image logoImage(int size) {
        return logo(size).getImage();
    }

    /**
     * Set the logo as the window icon (task bar + title bar) on a frame.
     * Convenience method — call from every JFrame's constructor.
     */
    public static void applyIcon(JFrame frame) {
        frame.setIconImage(logoImage(64));
    }

    /**
     * If the PNG is missing (e.g. someone deleted it), draw a simple
     * blue shield with a white cross so the app still looks OK.
     */
    private static Image fallbackLogo() {
        if (FALLBACK_LOGO != null) return FALLBACK_LOGO;
        int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 102, 178));
        int[] xs = { size/2, size-10, size-10, size/2, 10, 10 };
        int[] ys = { 5, 25, size/2, size-5, size/2, 25 };
        g.fillPolygon(xs, ys, 6);
        g.setColor(Color.WHITE);
        int cx = size / 2;
        g.fillRect(cx - 10, 40, 20, 50);
        g.fillRect(cx - 25, 55, 50, 20);
        g.dispose();
        FALLBACK_LOGO = img;
        return FALLBACK_LOGO;
    }

    /** Utility: apply system look-and-feel once at startup. */
    public static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
    }

    static { Objects.requireNonNull(PROPS); }
}
