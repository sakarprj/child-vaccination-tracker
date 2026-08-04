package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class StatusPill extends JLabel {

    private final Color fg;
    private final Color bg;

    public StatusPill(String text, Color fg, Color bg) {
        super(text);
        this.fg = fg;
        this.bg = bg;
        setFont(UI.bold(11.5f));
        setForeground(fg);
        setHorizontalAlignment(CENTER);
        setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight();
        FontMetrics fm = g2.getFontMetrics(getFont());
        int textW = fm.stringWidth(getText());
        int padH = 12;
        int pillW = textW + padH * 2;
        int x = (getWidth() - pillW) / 2;
        int y = (h - fm.getHeight()) / 2;

        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Double(x, y, pillW, fm.getHeight(),
                fm.getHeight(), fm.getHeight()));
        g2.setColor(fg);
        g2.setFont(getFont());
        g2.drawString(getText(), x + padH, y + fm.getAscent() - 1);
        g2.dispose();
    }

    public static DefaultTableCellRenderer tableRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                                                           boolean sel, boolean focus, int row, int col) {
                String s = v == null ? "" : v.toString();
                Color fg, bg;
                switch (s.toUpperCase()) {
                    case "COMPLETED", "ACTIVE" -> { fg = UI.SUCCESS; bg = UI.SUCCESS_BG; }
                    case "MISSED",   "INACTIVE" -> { fg = UI.DANGER;  bg = UI.DANGER_BG; }
                    default -> { fg = UI.WARN; bg = UI.WARN_BG; }
                }
                JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                wrap.setBackground(sel ? UI.PRIMARY_LIGHT :
                        (row % 2 == 0 ? Color.WHITE : UI.ROW_ALT));
                wrap.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                StatusPill pill = new StatusPill(s, fg, bg);
                pill.setPreferredSize(new Dimension(100, 22));
                wrap.add(pill);
                return wrap;
            }
        };
    }
}