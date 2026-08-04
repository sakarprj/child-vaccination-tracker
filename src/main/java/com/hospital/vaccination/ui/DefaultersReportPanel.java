package com.hospital.vaccination.ui;

import com.hospital.vaccination.dao.DefaultersDAO;
import com.hospital.vaccination.dao.DefaultersDAO.DefaulterRow;
import com.hospital.vaccination.ui.components.EmptyState;
import com.hospital.vaccination.ui.components.PageHeader;
import com.hospital.vaccination.ui.components.PremiumTable;
import com.hospital.vaccination.ui.components.RoundedButton;
import com.hospital.vaccination.ui.components.RoundedButton.Style;
import com.hospital.vaccination.ui.components.StatCard;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.DateUtil;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DefaultersReportPanel extends JPanel {

    private final DefaultersDAO dao = new DefaultersDAO();
    private final DefaulterTableModel model = new DefaulterTableModel();
    private final PremiumTable table = new PremiumTable(model);

    private final StatCard cardDefaulters = new StatCard("Defaulting children", "0", Icons.TRIANGLE_WARN, UI.DANGER);
    private final StatCard cardMissed     = new StatCard("Total missed doses",  "0", Icons.SYRINGE,       UI.WARN);

    private JPanel tableCard;

    public DefaultersReportPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG_APP);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        int[] w = { 170, 100, 160, 120, 200, 70, 220 };
        for (int i = 0; i < w.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        refresh();
    }

    private PageHeader buildHeader() {
        PageHeader h = new PageHeader("Defaulters Report",
                "Children with one or more MISSED vaccinations - priority follow-ups.");
        RoundedButton refresh = new RoundedButton("Refresh", Style.SECONDARY).withIcon(Icons.REFRESH);
        refresh.addActionListener(e -> refresh());
        RoundedButton export = new RoundedButton("Export CSV", Style.PRIMARY).withIcon(Icons.FILE_CSV);
        export.addActionListener(e -> onExportCsv());
        h.addAction(refresh); h.addAction(export);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(UI.BG_APP);
        body.setBorder(UI.padding(24, 32));

        JPanel stats = new JPanel(new GridLayout(1, 2, 14, 0));
        stats.setOpaque(false);
        stats.add(cardDefaulters); stats.add(cardMissed);
        body.add(stats, BorderLayout.NORTH);

        tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UI.CARD_BG);
        tableCard.setBorder(BorderFactory.createLineBorder(UI.BORDER, 1, true));
        setTableView();
        body.add(tableCard, BorderLayout.CENTER);
        return body;
    }

    private void setTableView() {
        tableCard.removeAll();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        tableCard.add(sp, BorderLayout.CENTER);
        tableCard.revalidate();
        tableCard.repaint();
    }

    private void setEmptyView() {
        tableCard.removeAll();
        tableCard.add(new EmptyState(Icons.CIRCLE_CHECK,
                        "No defaulters - every child is up to date",
                        "Great work! There are no children with missed doses right now."),
                BorderLayout.CENTER);
        tableCard.revalidate();
        tableCard.repaint();
    }

    public void refreshExternally() { refresh(); }

    private void refresh() {
        try {
            List<DefaulterRow> rows = dao.findAll();
            model.setRows(rows);
            int totalMissed = rows.stream().mapToInt(DefaulterRow::missedCount).sum();
            cardDefaulters.setValue(String.valueOf(rows.size()));
            cardMissed    .setValue(String.valueOf(totalMissed));
            if (rows.isEmpty()) setEmptyView(); else setTableView();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Could not load report: " + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExportCsv() {
        if (model.rows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to export.",
                    "Empty report", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export defaulters report");
        chooser.setSelectedFile(new File("defaulters_" + LocalDate.now() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".csv")) {
            out = new File(out.getParentFile(), out.getName() + ".csv");
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("Child Name,DOB (AD),DOB (BS),Parent,Phone,Address,Missed Count,Missed Vaccines");
            for (DefaulterRow r : model.rows) {
                pw.println(String.join(",",
                        csv(r.childName()),
                        csv(DateUtil.display(r.childDob())),
                        csv(BSDateConverter.toBS(r.childDob()).format()),
                        csv(r.parentName()),
                        csv(r.parentPhone()),
                        csv(r.address()),
                        String.valueOf(r.missedCount()),
                        csv(r.missedVaccines())));
            }
            JOptionPane.showMessageDialog(this,
                    "Exported " + model.rows.size() + " rows to:\n" + out.getAbsolutePath(),
                    "CSV saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Could not save CSV: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csv(String s) {
        if (s == null) return "";
        boolean q = s.contains(",") || s.contains("\"") || s.contains("\n");
        String v = s.replace("\"", "\"\"");
        return q ? "\"" + v + "\"" : v;
    }

    private static class DefaulterTableModel extends AbstractTableModel {
        private final String[] COLS = {
                "Child name", "DOB", "Parent", "Phone", "Address", "Missed", "Missed vaccines"
        };
        List<DefaulterRow> rows = new ArrayList<>();
        void setRows(List<DefaulterRow> rows) { this.rows = rows; fireTableDataChanged(); }
        @Override public int getRowCount()           { return rows.size(); }
        @Override public int getColumnCount()        { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public Object getValueAt(int r, int c) {
            DefaulterRow d = rows.get(r);
            return switch (c) {
                case 0 -> d.childName();
                case 1 -> DateUtil.display(d.childDob());
                case 2 -> d.parentName();
                case 3 -> d.parentPhone();
                case 4 -> d.address() == null ? "" : d.address();
                case 5 -> d.missedCount();
                case 6 -> d.missedVaccines();
                default -> "";
            };
        }
        @Override public Class<?> getColumnClass(int c) { return c == 5 ? Integer.class : String.class; }
    }
}