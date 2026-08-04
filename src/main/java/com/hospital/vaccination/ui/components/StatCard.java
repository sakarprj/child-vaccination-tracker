package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class StatCard extends JPanel {

    private static final int ARC = 14;

    private final JLabel labelL;
    private final JLabel valueL;
    private final JLabel subtitleL;
    private final String iconGlyph;
    private final Color  accent;

    public StatCard(String label, String value, String iconGlyph, Color accent) {
        this.iconGlyph = iconGlyph;
        this.accent = accent;
        setOpaque(false);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(new IconDisc(iconGlyph, accent), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        labelL = new JLabel(label.toUpperCase());
        labelL.setFont(UI.caps());
        labelL.setForeground(UI.TEXT_SECONDARY);
        labelL.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueL = new JLabel(value);
        valueL.setFont(UI.statNumber());
        valueL.setForeground(UI.TEXT_PRIMARY);
        valueL.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleL = new JLabel(" ");
        subtitleL.setFont(UI.small());
        subtitleL.setForeground(UI.TEXT_MUTED);
        subtitleL.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(labelL);
        text.add(Box.createVerticalStrut(4));
        text.add(valueL);
        text.add(subtitleL);
        add(text, BorderLayout.CENTER);
    }

    public void setValue(String v)    { valueL.setText(v); }
    public void setSubtitle(String s) { subtitleL.setText(s == null ? " " : s); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        g2.setColor(new Color(15, 30, 61, 14));
        g2.fill(new RoundRectangle2D.Double(1, 3, w - 2, h - 3, ARC, ARC));

        g2.setColor(UI.CARD_BG);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h - 2, ARC, ARC));

        g2.setColor(UI.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1.5, h - 2.5, ARC, ARC));

        g2.dispose();
    }

    private static class IconDisc extends JComponent {
        private final String glyph;
        private final Color  color;

        IconDisc(String glyph, Color color) {
            this.glyph = glyph;
            this.color = color;
            setPreferredSize(new Dimension(46, 46));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
            g2.fillOval(0, 0, d, d);

            g2.setColor(color);
            g2.setFont(UI.icon(20f));
            FontMetrics fm = g2.getFontMetrics();
            int gw = fm.stringWidth(glyph);
            g2.drawString(glyph, (d - gw) / 2, (d + fm.getAscent()) / 2 - 3);

            g2.dispose();
        }
    }
}