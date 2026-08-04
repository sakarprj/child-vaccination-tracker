package com.hospital.vaccination.ui.components;

import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;

public class PremiumTable extends JTable {

    public PremiumTable(TableModel model) {
        super(model);
        applyStyling();
    }

    private void applyStyling() {
        setRowHeight(36);
        setShowGrid(false);
        setShowHorizontalLines(true);
        setGridColor(UI.BORDER);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setSelectionBackground(UI.PRIMARY_LIGHT);
        setSelectionForeground(UI.TEXT_PRIMARY);
        setFont(UI.body());
        setAutoCreateRowSorter(true);
        setBackground(Color.WHITE);

        JTableHeader h = getTableHeader();
        h.setDefaultRenderer(new HeaderRenderer());
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 44));
        h.setReorderingAllowed(false);
        h.setBorder(BorderFactory.createEmptyBorder());

        setDefaultRenderer(Object.class,  new ZebraRenderer(SwingConstants.LEFT));
        setDefaultRenderer(String.class,  new ZebraRenderer(SwingConstants.LEFT));
        setDefaultRenderer(Integer.class, new ZebraRenderer(SwingConstants.CENTER));
        setDefaultRenderer(Boolean.class, new CheckRenderer());
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setHorizontalAlignment(LEFT);
            setForeground(Color.WHITE);
            setBackground(UI.SIDEBAR_TOP);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
            setFont(UI.bold(12f));
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                                                       boolean sel, boolean focus, int row, int col) {
            setText(v == null ? "" : v.toString().toUpperCase());
            setBackground(UI.SIDEBAR_TOP);
            setForeground(Color.WHITE);
            return this;
        }
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        ZebraRenderer(int align) {
            setHorizontalAlignment(align);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                                                       boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            if (!sel) {
                setBackground(row % 2 == 0 ? Color.WHITE : UI.ROW_ALT);
                setForeground(UI.TEXT_PRIMARY);
            }
            return this;
        }
    }

    private static class CheckRenderer extends JCheckBox implements javax.swing.table.TableCellRenderer {
        CheckRenderer() { setHorizontalAlignment(CENTER); setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                                                       boolean sel, boolean focus, int row, int col) {
            setSelected(Boolean.TRUE.equals(v));
            setBackground(sel ? UI.PRIMARY_LIGHT
                    : (row % 2 == 0 ? Color.WHITE : UI.ROW_ALT));
            return this;
        }
    }

    public void useStatusPillOn(int col) {
        getColumnModel().getColumn(col).setCellRenderer(StatusPill.tableRenderer());
    }
}