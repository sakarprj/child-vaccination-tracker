package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ShadowPanel extends JPanel {

    private static final int SHADOW_SIZE = 8;
    private static final int ARC = 14;

    private final JPanel content;

    public ShadowPanel() { this(16); }

    public ShadowPanel(int innerPadding) {
        super(new BorderLayout());
        setOpaque(false);
        super.setBorder(BorderFactory.createEmptyBorder(
                SHADOW_SIZE / 2, SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE));

        content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(
                innerPadding, innerPadding, innerPadding, innerPadding));
        super.add(content, BorderLayout.CENTER);
    }

    @Override
    public Component add(Component comp) { return content.add(comp); }

    public void add(Component comp, Object constraints) { content.add(comp, constraints); }

    @Override
    public void setLayout(LayoutManager mgr) {
        if (content != null) content.setLayout(mgr);
        else super.setLayout(mgr);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int shadowOffset = 3;
        int cardX = SHADOW_SIZE;
        int cardY = SHADOW_SIZE / 2;
        int cardW = w - SHADOW_SIZE * 2;
        int cardH = h - SHADOW_SIZE - SHADOW_SIZE / 2;

        for (int i = SHADOW_SIZE; i > 0; i--) {
            int alpha = 4 + (SHADOW_SIZE - i) * 3;
            g2.setColor(new Color(15, 30, 61, alpha));
            g2.fill(new RoundRectangle2D.Double(
                    cardX - i / 2.0,
                    cardY - i / 2.0 + shadowOffset,
                    cardW + i, cardH + i,
                    ARC + i, ARC + i));
        }

        g2.setColor(UI.CARD_BG);
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, ARC, ARC));

        g2.setColor(UI.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW - 1, cardH - 1, ARC, ARC));

        g2.dispose();
    }
}