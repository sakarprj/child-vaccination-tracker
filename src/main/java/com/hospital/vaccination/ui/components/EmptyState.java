package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;

public class EmptyState extends JPanel {

    public EmptyState(String iconGlyph, String headline, String subtext) {
        setBackground(UI.CARD_BG);
        setLayout(new GridBagLayout());

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(iconGlyph, SwingConstants.CENTER);
        icon.setFont(UI.icon(48f));
        icon.setForeground(UI.TEXT_MUTED);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(icon);
        col.add(Box.createVerticalStrut(12));

        JLabel head = new JLabel(headline, SwingConstants.CENTER);
        head.setFont(UI.bold(15f));
        head.setForeground(UI.TEXT_PRIMARY);
        head.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(head);

        if (subtext != null && !subtext.isBlank()) {
            col.add(Box.createVerticalStrut(4));
            JLabel sub = new JLabel(subtext, SwingConstants.CENTER);
            sub.setFont(UI.small());
            sub.setForeground(UI.TEXT_SECONDARY);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);
            col.add(sub);
        }

        add(col);
    }
}