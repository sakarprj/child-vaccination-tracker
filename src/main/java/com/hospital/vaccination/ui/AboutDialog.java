package com.hospital.vaccination.ui;

import com.hospital.vaccination.config.AppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * "About" dialog reachable from Help → About on any dashboard.
 * Non-modal, dispose-on-close, keyboard-friendly (Esc closes).
 */
public class AboutDialog extends JDialog {

    public AboutDialog(Window owner) {
        super(owner, "About", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(20, 28, 16, 28));
        root.setBackground(Color.WHITE);

        // Logo
        JLabel logo = new JLabel(AppConfig.logo(80));
        root.add(logo, BorderLayout.WEST);

        // Text block
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(AppConfig.hospitalName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 18f));

        JLabel ver = new JLabel("Version " + AppConfig.appVersion());
        ver.setForeground(Color.GRAY);

        JLabel line = new JLabel("Child Vaccination Management System");

        JLabel schedule = new JLabel(AppConfig.hospitalTagline());
        schedule.setForeground(Color.GRAY);

        JLabel builtWith = new JLabel(
                "<html><small>Built with Java Swing, MySQL, Apache PDFBox.</small></html>");
        builtWith.setForeground(Color.GRAY);

        text.add(name);
        text.add(Box.createVerticalStrut(2));
        text.add(ver);
        text.add(Box.createVerticalStrut(10));
        text.add(line);
        text.add(schedule);
        text.add(Box.createVerticalStrut(8));
        text.add(builtWith);

        root.add(text, BorderLayout.CENTER);

        // Close button
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setOpaque(false);
        btns.add(close);
        root.add(btns, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(close);

        // Esc closes
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(owner);
    }
}
