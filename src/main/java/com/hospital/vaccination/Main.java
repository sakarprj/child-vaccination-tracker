package com.hospital.vaccination;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.config.DBBootstrap;
import com.hospital.vaccination.config.DBConnection;
import com.hospital.vaccination.service.ConsoleSmsService;
import com.hospital.vaccination.service.ReminderJob;
import com.hospital.vaccination.ui.LoginFrame;
import com.hospital.vaccination.ui.SplashScreen;

import javax.swing.*;

/**
 * Application entry point.
 *
 * <ol>
 *   <li>Set the system look-and-feel so Swing feels native.</li>
 *   <li>Show branded splash screen.</li>
 *   <li>Verify DB connectivity — bail with a friendly dialog if not.</li>
 *   <li>Run one-time bootstrap (fix placeholder password hashes).</li>
 *   <li>Start the console SMS reminder simulator and reminder job.</li>
 *   <li>Dismiss splash and show the login screen.</li>
 * </ol>
 */
public class Main {

    /** Kept as a static field so the reminder daemon is not garbage-collected. */
    private static ReminderJob reminderJob;

    public static void main(String[] args) {
        AppConfig.applySystemLookAndFeel();
        com.hospital.vaccination.util.UI.install();

        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = SplashScreen.display();

            // Small delay so the splash renders before blocking startup work.
            Timer timer = new Timer(400, e -> continueStartup(splash));
            timer.setRepeats(false);
            timer.start();
        });
    }

    private static void continueStartup(SplashScreen splash) {
        if (!DBConnection.canConnect()) {
            splash.close();

            JOptionPane.showMessageDialog(
                    null,
                    "Could not connect to MySQL.\n\n"
                            + "1. Start MySQL from the XAMPP Control Panel.\n"
                            + "2. Run db/schema.sql once in phpMyAdmin.\n"
                            + "3. Check src/main/resources/db.properties.",
                    "Database connection failed",
                    JOptionPane.ERROR_MESSAGE
            );

            System.exit(1);
        }

        try {
            DBBootstrap.run();
        } catch (RuntimeException ex) {
            splash.close();
            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Startup bootstrap failed:\n" + ex.getMessage(),
                    "Startup error",
                    JOptionPane.ERROR_MESSAGE
            );

            System.exit(1);
        }

        // Console only: no real/external SMS API is used in this project.
        reminderJob = new ReminderJob(new ConsoleSmsService());
        reminderJob.start();

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> reminderJob.stop(), "reminder-shutdown")
        );

        Timer showLogin = new Timer(600, e -> {
            splash.close();
            new LoginFrame().setVisible(true);
        });

        showLogin.setRepeats(false);
        showLogin.start();
    }
}