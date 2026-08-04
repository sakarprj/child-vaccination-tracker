package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;

public class PageHeader extends JPanel {

    private final JPanel actionArea;

    public PageHeader(String title, String subtitle) {
        setLayout(new BorderLayout());
        setBackground(UI.CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UI.BORDER),
                BorderFactory.createEmptyBorder(22, 32, 22, 32)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel titleL = new JLabel(title);
        titleL.setFont(UI.h2());
        titleL.setForeground(UI.TEXT_PRIMARY);
        titleL.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(titleL);

        if (subtitle != null && !subtitle.isBlank()) {
            left.add(Box.createVerticalStrut(4));
            JLabel subL = new JLabel(subtitle);
            subL.setFont(UI.small());
            subL.setForeground(UI.TEXT_SECONDARY);
            subL.setAlignmentX(Component.LEFT_ALIGNMENT);
            left.add(subL);
        }
        add(left, BorderLayout.WEST);

        actionArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionArea.setOpaque(false);
        add(actionArea, BorderLayout.EAST);
    }

    public void addAction(Component c) {
        actionArea.add(c);
        actionArea.revalidate();
    }
}