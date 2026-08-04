package com.hospital.vaccination.ui;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.model.User;
import com.hospital.vaccination.service.AuthService;
import com.hospital.vaccination.ui.components.RoundedButton;
import com.hospital.vaccination.ui.components.RoundedButton.Style;
import com.hospital.vaccination.ui.components.RoundedTextField;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;

/**
 * Premium login window. Left half: brand hero panel with gradient.
 * Right half: white card with the sign-in form.
 */
public class LoginFrame extends JFrame {

    private final RoundedTextField usernameField = new RoundedTextField(20);
    private final JPasswordField   passwordField = new JPasswordField(20);
    private final JLabel           statusLabel   = new JLabel(" ");
    private final AuthService      authService   = new AuthService();

    public LoginFrame() {
        super(AppConfig.windowTitle("Login"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        AppConfig.applyIcon(this);
        setSize(980, 640);
        setMinimumSize(new Dimension(720, 560));
        RoundedTextField.styleAsRounded(passwordField);
        buildUI();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(UI.BG_APP);
        root.add(new HeroPanel());
        root.add(buildFormPanel());
        setContentPane(root);
    }

    // ---- right: form panel -------------------------------------------------

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UI.BG_APP);

        JPanel card = new JPanel();
        card.setBackground(UI.CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UI.BORDER, 1, true),
                UI.padding(48, 48)));
        card.setPreferredSize(new Dimension(420, 430));
        card.setMaximumSize(new Dimension(420, 430));

        JLabel title = new JLabel("Welcome back");
        title.setFont(UI.h1());
        title.setForeground(UI.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Sign in to your staff account");
        sub.setFont(UI.small());
        sub.setForeground(UI.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(32));

        card.add(labeled("Username", usernameField));
        card.add(Box.createVerticalStrut(18));
        card.add(labeled("Password", passwordField));
        card.add(Box.createVerticalStrut(10));

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setForeground(UI.DANGER);
        statusLabel.setFont(UI.small());
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(14));

        RoundedButton loginBtn = new RoundedButton("Login", Style.PRIMARY).withIcon(Icons.LOCK);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginBtn.setPreferredSize(new Dimension(0, 46));
        loginBtn.addActionListener(e -> attemptLogin());
        card.add(loginBtn);
        getRootPane().setDefaultButton(loginBtn);

        // Enter submits from either field
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attemptLogin();
            }
        };
        usernameField.addKeyListener(enter);
        passwordField.addKeyListener(enter);

        outer.add(card);
        return outer;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setFont(UI.bold(11.5f));
        l.setForeground(UI.TEXT_PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setPreferredSize(new Dimension(0, 40));
        p.add(field);
        return p;
    }

    // ---- login logic -------------------------------------------------------

    private void attemptLogin() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());

        setStatus(" ", UI.DANGER);

        Optional<User> result;
        try { result = authService.login(user, pass); }
        catch (RuntimeException ex) {
            setStatus("Database error — see console", UI.DANGER);
            ex.printStackTrace();
            return;
        }

        if (result.isEmpty()) {
            setStatus("Invalid username or password", UI.DANGER);
            passwordField.setText("");
            passwordField.requestFocus();
            return;
        }

        User u = result.get();
        JFrame dashboard = switch (u.getRole()) {
            case ADMIN -> new AdminDashboard();
            case NURSE -> new NurseDashboard();
        };
        dashboard.setVisible(true);
        dispose();
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    // ================================================== HERO PANEL ========

    /** Left panel with gradient, logo, and hospital name. */
    private static class HeroPanel extends JPanel {
        HeroPanel() {
            setLayout(new GridBagLayout());
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Vertical gradient
            g2.setPaint(new GradientPaint(
                    0, 0,       UI.SIDEBAR_TOP,
                    0, h,       UI.SIDEBAR_BOTTOM));
            g2.fillRect(0, 0, w, h);

            // Big translucent shield behind content (decorative)
            g2.setColor(new Color(255, 255, 255, 12));
            int d = Math.min(w, h) * 3 / 2;
            g2.fill(new RoundRectangle2D.Double(-d / 3.0, h - d / 2.0, d, d, d, d));

            // Gold accent stripe
            g2.setColor(UI.ACCENT_GOLD);
            g2.fillRect(0, h / 2 - 40, 4, 80);

            g2.dispose();

            super.paintComponent(g);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (getComponentCount() > 0) return;

            JPanel col = new JPanel();
            col.setOpaque(false);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

            JLabel logo = new JLabel(AppConfig.logo(96));
            logo.setAlignmentX(Component.LEFT_ALIGNMENT);
            col.add(logo);
            col.add(Box.createVerticalStrut(24));

            JLabel brand = new JLabel(AppConfig.hospitalName());
            brand.setFont(UI.bold(28f));
            brand.setForeground(Color.WHITE);
            brand.setAlignmentX(Component.LEFT_ALIGNMENT);
            col.add(brand);
            col.add(Box.createVerticalStrut(6));

            JLabel tag = new JLabel("<html>" + AppConfig.hospitalTagline() + "</html>");
            tag.setFont(UI.base(14f));
            tag.setForeground(UI.TEXT_ON_DARK_M);
            tag.setAlignmentX(Component.LEFT_ALIGNMENT);
            col.add(tag);
            col.add(Box.createVerticalStrut(40));

            col.add(featureRow(Icons.SYRINGE,     "Auto-generated Nepal NIP schedule"));
            col.add(Box.createVerticalStrut(14));
            col.add(featureRow(Icons.CALENDAR,    "AD & BS calendar throughout"));
            col.add(Box.createVerticalStrut(14));
            col.add(featureRow(Icons.HEART_PULSE, "SMS reminders for every dose"));
            col.add(Box.createVerticalStrut(14));
            col.add(featureRow(Icons.PRINT,       "Printable vaccination cards"));

            JPanel wrap = new JPanel();
            wrap.setOpaque(false);
            wrap.setBorder(UI.padding(0, 60));
            wrap.add(col);
            add(wrap);
        }

        private JPanel featureRow(String icon, String text) {
            JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            r.setOpaque(false);
            r.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel i = new JLabel(icon);
            i.setFont(UI.icon(18f));
            i.setForeground(UI.ACCENT_GOLD);
            r.add(i);

            JLabel t = new JLabel(text);
            t.setForeground(Color.WHITE);
            t.setFont(UI.base(13.5f));
            r.add(t);
            return r;
        }
    }
}
