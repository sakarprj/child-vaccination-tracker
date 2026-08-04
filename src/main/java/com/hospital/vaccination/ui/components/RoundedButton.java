package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class RoundedButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, DANGER, GHOST }

    private static final int ARC = 10;
    private final Style style;
    private String iconGlyph;
    private float hoverT = 0f;
    private Timer animator;

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(fg());
        setFont(UI.bold(13f));
        setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mh = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { animateTo(1f); }
            @Override public void mouseExited (MouseEvent e) { animateTo(0f); }
        };
        addMouseListener(mh);
    }

    public RoundedButton withIcon(String glyph) {
        this.iconGlyph = glyph;
        return this;
    }

    private void animateTo(float target) {
        if (animator != null) animator.stop();
        animator = new Timer(16, e -> {
            float step = 0.15f;
            if (Math.abs(hoverT - target) < step) { hoverT = target; ((Timer) e.getSource()).stop(); }
            else hoverT += (target > hoverT ? step : -step);
            repaint();
        });
        animator.start();
    }

    private Color bg() {
        return switch (style) {
            case PRIMARY   -> UI.PRIMARY;
            case DANGER    -> UI.DANGER;
            case SECONDARY -> UI.CARD_BG;
            case GHOST     -> new Color(0, 0, 0, 0);
        };
    }
    private Color bgHover() {
        return switch (style) {
            case PRIMARY   -> UI.PRIMARY_HOVER;
            case DANGER    -> new Color(200, 40, 55);
            case SECONDARY -> UI.PRIMARY_LIGHT;
            case GHOST     -> new Color(15, 30, 61, 12);
        };
    }
    private Color fg() {
        return switch (style) {
            case PRIMARY, DANGER -> Color.WHITE;
            case SECONDARY       -> UI.PRIMARY;
            case GHOST           -> UI.TEXT_PRIMARY;
        };
    }
    private Color border() {
        return switch (style) {
            case SECONDARY -> UI.BORDER;
            default        -> null;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        Color base = bg(), hover = bgHover();
        int r = (int) (base.getRed()   + (hover.getRed()   - base.getRed())   * hoverT);
        int gc = (int) (base.getGreen() + (hover.getGreen() - base.getGreen()) * hoverT);
        int b = (int) (base.getBlue()  + (hover.getBlue()  - base.getBlue())  * hoverT);
        int a = (int) (base.getAlpha() + (hover.getAlpha() - base.getAlpha()) * hoverT);
        g2.setColor(new Color(r, gc, b, a));
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, ARC, ARC));

        Color bord = border();
        if (bord != null) {
            g2.setColor(bord);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, ARC, ARC));
        }

        FontMetrics textFm = g2.getFontMetrics(getFont());
        int textW = textFm.stringWidth(getText());
        int iconW = 0;
        FontMetrics iconFm = null;
        if (iconGlyph != null) {
            Font iconFont = UI.icon(getFont().getSize2D());
            iconFm = g2.getFontMetrics(iconFont);
            iconW = iconFm.stringWidth(iconGlyph) + 8;
        }
        int totalW = iconW + textW;
        int startX = (w - totalW) / 2;

        g2.setColor(fg());

        if (iconGlyph != null) {
            g2.setFont(UI.icon(getFont().getSize2D()));
            int iy = (h + iconFm.getAscent()) / 2 - 2;
            g2.drawString(iconGlyph, startX, iy);
            startX += iconW;
        }

        g2.setFont(getFont());
        int ty = (h + textFm.getAscent()) / 2 - 2;
        g2.drawString(getText(), startX, ty);

        g2.dispose();
    }

    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 36));
    }
}