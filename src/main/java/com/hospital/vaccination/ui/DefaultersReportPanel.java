package com.hospital.vaccination.ui;

import com.hospital.vaccination.dao.DefaultersDAO;
import com.hospital.vaccination.dao.DefaultersDAO.DefaulterRow;
import com.hospital.vaccination.service.AuthService;
import com.hospital.vaccination.ui.components.EmptyState;
import com.hospital.vaccination.ui.components.PageHeader;
import com.hospital.vaccination.ui.components.PremiumTable;
import com.hospital.vaccination.ui.components.RoundedButton;
import com.hospital.vaccination.ui.components.RoundedButton.Style;
import com.hospital.vaccination.ui.components.RoundedTextField;
import com.hospital.vaccination.ui.components.StatCard;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.DateUtil;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared staff report for children with missed vaccination doses. */
public class DefaultersReportPanel extends JPanel {

    private final DefaultersDAO dao = new DefaultersDAO();
    private final DefaulterTableModel model = new DefaulterTableModel();
    private final PremiumTable table = new PremiumTable(model);
    private final RoundedTextField searchField = new RoundedTextField(24);

    /** Full report data; the table model contains the current filtered view. */
    private List<DefaulterRow> allRows = new ArrayList<>();

    private final StatCard cardDefaulters = new StatCard(
            "Defaulting children", "0", Icons.TRIANGLE_WARN, UI.DANGER);
    private final StatCard cardMissed = new StatCard(
            "Total missed doses", "0", Icons.SYRINGE, UI.WARN);

    private JPanel tableCard;

    public DefaultersReportPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG_APP);

        searchField.setPlaceholder("Search child, parent, phone, or vaccine…");

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        int[] widths = {170, 100, 160, 120, 200, 70, 220};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        refresh();
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader(
                "Defaulters Report",
                "Children with one or more MISSED vaccinations — priority follow-ups.");

        RoundedButton refresh = new RoundedButton(
                "Refresh", Style.SECONDARY).withIcon(Icons.REFRESH);
        refresh.addActionListener(e -> refresh());

        RoundedButton export = new RoundedButton(
                "Export CSV", Style.PRIMARY).withIcon(Icons.FILE_CSV);
        export.addActionListener(e -> onExportCsv());

        header.addAction(refresh);
        header.addAction(export);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(UI.BG_APP);
        body.setBorder(UI.padding(24, 32));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(buildSearchRow(), BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 2, 14, 0));
        stats.setOpaque(false);
        stats.add(cardDefaulters);
        stats.add(cardMissed);
        top.add(stats, BorderLayout.SOUTH);
        body.add(top, BorderLayout.NORTH);

        tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UI.CARD_BG);
        tableCard.setBorder(BorderFactory.createLineBorder(UI.BORDER, 1, true));
        body.add(tableCard, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildSearchRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);

        JLabel icon = new JLabel(Icons.SEARCH);
        icon.setFont(UI.icon(14f));
        icon.setForeground(UI.TEXT_SECONDARY);
        row.add(icon);

        searchField.setPreferredSize(new Dimension(350, 36));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        row.add(searchField);

        return row;
    }

    private void setTableView() {
        tableCard.removeAll();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.revalidate();
        tableCard.repaint();
    }

    private void setEmptyView() {
        tableCard.removeAll();
        tableCard.add(new EmptyState(
                        Icons.CIRCLE_CHECK,
                        "No defaulters — every child is up to date",
                        "Great work! There are no children with missed doses right now."),
                BorderLayout.CENTER);
        tableCard.revalidate();
        tableCard.repaint();
    }

    private void setNoMatchView() {
        tableCard.removeAll();
        tableCard.add(new EmptyState(
                        Icons.SEARCH,
                        "No matching defaulters found",
                        "Try a different child name, parent name, phone number, or vaccine."),
                BorderLayout.CENTER);
        tableCard.revalidate();
        tableCard.repaint();
    }

    public void refreshExternally() {
        refresh();
    }

    public void focusSearch() {
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    private void refresh() {
        AuthService.touch();
        try {
            allRows = dao.findAll();
            applyFilter();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            showError("Could not load report: " + ex.getMessage());
        }
    }

    /** Live, client-side search over the loaded defaulters report. */
    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<DefaulterRow> shown = allRows.stream()
                .filter(row -> matches(row, query))
                .toList();

        model.setRows(shown);
        updateStats(shown);

        if (allRows.isEmpty()) {
            setEmptyView();
        } else if (shown.isEmpty()) {
            setNoMatchView();
        } else {
            setTableView();
        }
    }

    private boolean matches(DefaulterRow row, String query) {
        if (query.isEmpty()) return true;
        return contains(row.childName(), query)
                || contains(row.parentName(), query)
                || contains(row.parentPhone(), query)
                || contains(row.missedVaccines(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void updateStats(List<DefaulterRow> rows) {
        int totalMissed = rows.stream().mapToInt(DefaulterRow::missedCount).sum();
        cardDefaulters.setValue(String.valueOf(rows.size()));
        cardMissed.setValue(String.valueOf(totalMissed));

        String suffix = rows.size() == 1 ? "child shown" : "children shown";
        cardDefaulters.setSubtitle(rows.size() + " " + suffix);
        cardMissed.setSubtitle("In current results");
    }

    private void showError(String message) {
        tableCard.removeAll();
        tableCard.add(new EmptyState(
                Icons.TRIANGLE_WARN,
                "Unable to load defaulters report",
                message), BorderLayout.CENTER);
        tableCard.revalidate();
        tableCard.repaint();
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
            for (DefaulterRow row : model.rows) {
                pw.println(String.join(",",
                        csv(row.childName()),
                        csv(DateUtil.display(row.childDob())),
                        csv(BSDateConverter.toBS(row.childDob()).format()),
                        csv(row.parentName()),
                        csv(row.parentPhone()),
                        csv(row.address()),
                        String.valueOf(row.missedCount()),
                        csv(row.missedVaccines())));
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

    private String csv(String text) {
        if (text == null) return "";
        boolean quoted = text.contains(",") || text.contains("\"") || text.contains("\n");
        String escaped = text.replace("\"", "\"\"");
        return quoted ? "\"" + escaped + "\"" : escaped;
    }

    private static class DefaulterTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Child name", "DOB", "Parent", "Phone", "Address", "Missed", "Missed vaccines"
        };

        List<DefaulterRow> rows = new ArrayList<>();

        void setRows(List<DefaulterRow> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DefaulterRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.childName();
                case 1 -> DateUtil.display(row.childDob());
                case 2 -> row.parentName();
                case 3 -> row.parentPhone();
                case 4 -> row.address() == null ? "" : row.address();
                case 5 -> row.missedCount();
                case 6 -> row.missedVaccines();
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 5 ? Integer.class : String.class;
        }
    }
}
