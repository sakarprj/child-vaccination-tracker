package com.hospital.vaccination.ui;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SplashScreen extends JWindow {

    private SplashScreen() {
        JPanel root = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, UI.SIDEBAR_TOP,
                        0, h, UI.SIDEBAR_BOTTOM));
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 18, 18));
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(28, 44, 28, 44));

        JLabel logo = new JLabel(AppConfig.logo(100), SwingConstants.CENTER);
        root.add(logo, BorderLayout.NORTH);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 4));
        text.setOpaque(false);
        JLabel title = new JLabel(AppConfig.hospitalName(), SwingConstants.CENTER);
        title.setFont(UI.bold(22f));
        title.setForeground(Color.WHITE);
        JLabel tag = new JLabel(AppConfig.hospitalTagline(), SwingConstants.CENTER);
        tag.setFont(UI.small());
        tag.setForeground(UI.TEXT_ON_DARK_M);
        text.add(title);
        text.add(tag);
        root.add(text, BorderLayout.CENTER);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(0, 4));
        bar.setBorderPainted(false);
        bar.setForeground(UI.ACCENT_GOLD);
        bar.setBackground(new Color(255, 255, 255, 40));

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(bar, BorderLayout.NORTH);
        JLabel ver = new JLabel("v" + AppConfig.appVersion(), SwingConstants.CENTER);
        ver.setFont(UI.small());
        ver.setForeground(UI.TEXT_ON_DARK_M);
        bottom.add(ver, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setSize(360, 320);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
    }

    public static SplashScreen display() {
        SplashScreen s = new SplashScreen();
        s.setVisible(true);
        return s;
    }

    public void close() { SwingUtilities.invokeLater(this::dispose); }
}