package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

public class RoundedTextField extends JTextField {

    private static final int ARC = 8;
    private String placeholder;
    private boolean focused;

    public RoundedTextField(int columns) { super(columns); init(); }
    public RoundedTextField() { super(); init(); }

    private void init() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        setFont(UI.body());
        setForeground(UI.TEXT_PRIMARY);
        setBackground(UI.CARD_BG);
        setCaretColor(UI.PRIMARY);
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
            @Override public void focusLost  (FocusEvent e) { focused = false; repaint(); }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, ARC, ARC));

        if (focused) {
            g2.setColor(new Color(15, 76, 129, 40));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Double(1.5, 1.5, w - 3, h - 3, ARC, ARC));
        }

        g2.setColor(focused ? UI.PRIMARY : UI.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, ARC, ARC));

        g2.dispose();

        super.paintComponent(g);

        if (placeholder != null && getText().isEmpty() && !focused) {
            Graphics2D pg = (Graphics2D) g.create();
            pg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pg.setFont(getFont());
            pg.setColor(UI.TEXT_MUTED);
            FontMetrics fm = pg.getFontMetrics();
            pg.drawString(placeholder, 12, (h + fm.getAscent()) / 2 - 2);
            pg.dispose();
        }
    }

    public static void styleAsRounded(JTextComponent c) {
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UI.BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        c.setBackground(UI.CARD_BG);
        c.setForeground(UI.TEXT_PRIMARY);
        c.setCaretColor(UI.PRIMARY);
    }
}