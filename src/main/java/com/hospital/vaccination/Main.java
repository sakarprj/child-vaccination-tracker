package com.hospital.vaccination;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.config.DBBootstrap;
import com.hospital.vaccination.config.DBConnection;
import com.hospital.vaccination.config.SmsConfig;
import com.hospital.vaccination.service.ReminderJob;
import com.hospital.vaccination.service.SmsService;
import com.hospital.vaccination.ui.LoginFrame;
import com.hospital.vaccination.ui.SplashScreen;

import javax.swing.*;

public class Main {

    private static ReminderJob reminderJob;

    public static void main(String[] args) {
        AppConfig.applySystemLookAndFeel();
        com.hospital.vaccination.util.UI.install();

        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = SplashScreen.display();

            Timer t = new Timer(400, e -> continueStartup(splash));
            t.setRepeats(false);
            t.start();
        });
    }

    private static void continueStartup(SplashScreen splash) {
        if (!DBConnection.canConnect()) {
            splash.close();
            JOptionPane.showMessageDialog(
                    null,
                    "Could not connect to MySQL.\n\n" +
                            "1. Start MySQL from the XAMPP Control Panel.\n" +
                            "2. Run db/schema.sql once in phpMyAdmin.\n" +
                            "3. Check src/main/resources/db.properties.",
                    "Database connection failed",
                    JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SmsService sms = SmsConfig.build();
        reminderJob = new ReminderJob(sms);
        reminderJob.start();
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> reminderJob.stop(), "reminder-shutdown"));

        Timer showLogin = new Timer(600, e -> {
            splash.close();
            new LoginFrame().setVisible(true);
        });
        showLogin.setRepeats(false);
        showLogin.start();
    }
}