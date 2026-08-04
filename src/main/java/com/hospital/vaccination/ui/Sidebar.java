package com.hospital.vaccination.ui;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.model.User;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Sidebar extends JPanel {

    public static final int WIDTH = 250;

    private final CardLayout cards = new CardLayout();
    private final JPanel     cardHost = new JPanel(cards);
    private final List<NavItem> navItems = new ArrayList<>();
    private final User          user;
    private final Runnable      onLogout;

    private JPanel navHolder;

    public Sidebar(User user, Runnable onLogout) {
        this.user = user;
        this.onLogout = onLogout;

        cardHost.setBackground(UI.BG_APP);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(WIDTH, 100));
        setOpaque(false);

        add(buildBrand(),  BorderLayout.NORTH);
        add(buildNavArea(),BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(
                0, 0,           UI.SIDEBAR_TOP,
                0, h * 0.85f,   UI.SIDEBAR_BOTTOM));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    private JPanel buildBrand() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(UI.padding(28, 24, 22, 24));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new JLabel(AppConfig.logo(42)));

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(AppConfig.hospitalName());
        name.setFont(UI.bold(15f));
        name.setForeground(UI.TEXT_ON_DARK);
        names.add(name);

        JLabel tag = new JLabel("Vaccination Suite");
        tag.setFont(UI.small());
        tag.setForeground(UI.TEXT_ON_DARK_M);
        names.add(tag);

        row.add(names);
        p.add(row);

        p.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 30));
        sep.setBackground(new Color(255, 255, 255, 30));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(sep);

        return p;
    }

    private JPanel buildNavArea() {
        navHolder = new JPanel();
        navHolder.setOpaque(false);
        navHolder.setLayout(new BoxLayout(navHolder, BoxLayout.Y_AXIS));
        navHolder.setBorder(UI.padding(12, 12, 12, 12));

        JLabel section = new JLabel("MAIN MENU");
        section.setFont(UI.caps());
        section.setForeground(new Color(255, 255, 255, 120));
        section.setBorder(UI.padding(8, 12, 12, 12));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        navHolder.add(section);

        JScrollPane sp = new JScrollPane(navHolder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    public void addNav(String label, String iconGlyph, JComponent panel) {
        String key = label + "-" + navItems.size();
        cardHost.add(panel, key);
        NavItem item = new NavItem(label, iconGlyph, key);
        navItems.add(item);
        navHolder.add(item);
        navHolder.add(Box.createVerticalStrut(4));
        if (navItems.size() == 1) selectByKey(key);
        revalidate();
        repaint();
    }

    public void selectByLabel(String label) {
        for (NavItem it : navItems) {
            if (it.label.equalsIgnoreCase(label)) { selectByKey(it.cardKey); return; }
        }
    }

    public JPanel getCardHost() { return cardHost; }

    private void selectByKey(String key) {
        for (NavItem it : navItems) it.setActive(it.cardKey.equals(key));
        cards.show(cardHost, key);
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(UI.padding(12, 20, 24, 20));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 30));
        sep.setBackground(new Color(255, 255, 255, 30));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(sep);
        p.add(Box.createVerticalStrut(16));

        JPanel chip = new JPanel(new BorderLayout(12, 0));
        chip.setOpaque(false);
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        chip.add(new AvatarLabel(user.getFullName()), BorderLayout.WEST);

        JPanel who = new JPanel();
        who.setOpaque(false);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));

        JLabel n = new JLabel(user.getFullName());
        n.setForeground(UI.TEXT_ON_DARK);
        n.setFont(UI.bold(13f));
        JLabel r = new JLabel(user.getRole() == User.Role.ADMIN ? "Administrator" : "Nurse");
        r.setForeground(UI.TEXT_ON_DARK_M);
        r.setFont(UI.small());
        who.add(n);
        who.add(r);
        chip.add(who, BorderLayout.CENTER);
        p.add(chip);

        p.add(Box.createVerticalStrut(14));

        JButton logout = signOutButton();
        logout.addActionListener(e -> onLogout.run());
        p.add(logout);

        return p;
    }

    private JButton signOutButton() {
        JButton b = new JButton("Sign out") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isRollover()
                        ? new Color(255, 255, 255, 45)
                        : new Color(255, 255, 255, 22);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(UI.TEXT_ON_DARK);
        b.setFont(UI.bodyBold());
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(UI.padding(8, 14));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return b;
    }

    private class NavItem extends JPanel {
        final String label;
        final String iconGlyph;
        final String cardKey;
        private boolean active;
        private boolean hover;

        NavItem(String label, String iconGlyph, String cardKey) {
            this.label = label;
            this.iconGlyph = iconGlyph;
            this.cardKey = cardKey;
            setOpaque(false);
            setLayout(new BorderLayout());
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(WIDTH, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

            JLabel textL = new JLabel(label);
            textL.setForeground(UI.TEXT_ON_DARK_M);
            textL.setFont(UI.nav());
            textL.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));
            add(textL, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { selectByKey(cardKey); }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            for (Component c : getComponents()) {
                if (c instanceof JLabel l) {
                    l.setForeground(active ? UI.TEXT_ON_DARK : UI.TEXT_ON_DARK_M);
                    l.setFont(active ? UI.bold(13.5f) : UI.nav());
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            if (active) {
                g2.setColor(UI.SIDEBAR_ACTIVE);
                g2.fillRoundRect(4, 3, w - 8, h - 6, 8, 8);
                g2.setColor(UI.ACCENT_GOLD);
                g2.fillRoundRect(0, 8, 3, h - 16, 3, 3);
            } else if (hover) {
                g2.setColor(UI.SIDEBAR_HOVER);
                g2.fillRoundRect(4, 3, w - 8, h - 6, 8, 8);
            }

            g2.setFont(UI.icon(15f));
            g2.setColor(active ? UI.TEXT_ON_DARK : UI.TEXT_ON_DARK_M);
            FontMetrics fm = g2.getFontMetrics();
            int iw = fm.stringWidth(iconGlyph);
            g2.drawString(iconGlyph, 22 - iw / 2, (h + fm.getAscent()) / 2 - 3);

            g2.dispose();
        }
    }

    private static class AvatarLabel extends JLabel {
        private final String initials;
        private final Color  ringColor;

        AvatarLabel(String fullName) {
            this.initials  = initials(fullName);
            this.ringColor = colorFor(fullName);
            setPreferredSize(new Dimension(40, 40));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            int x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;

            g2.setPaint(new GradientPaint(x, y, ringColor.brighter(),
                    x + d, y + d, ringColor.darker()));
            g2.fillOval(x, y, d, d);

            g2.setColor(Color.WHITE);
            g2.setFont(UI.bold(13f));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(initials);
            g2.drawString(initials, x + (d - tw) / 2, y + (d + fm.getAscent()) / 2 - 3);

            g2.dispose();
        }

        private static String initials(String fullName) {
            if (fullName == null || fullName.isBlank()) return "?";
            String[] parts = fullName.trim().split("\\s+");
            String first = parts[0].substring(0, 1).toUpperCase();
            String last  = parts.length > 1
                    ? parts[parts.length - 1].substring(0, 1).toUpperCase() : "";
            return first + last;
        }

        private static Color colorFor(String s) {
            int h = (s == null ? 0 : s.hashCode()) & 0xFFFFFF;
            float hue = (h % 360) / 360f;
            return Color.getHSBColor(hue, 0.55f, 0.65f);
        }
    }
}