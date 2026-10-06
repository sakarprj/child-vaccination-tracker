package com.hospital.vaccination.ui;

import com.hospital.vaccination.dao.VaccinationRecordDAO;
import com.hospital.vaccination.dao.VaccinationRecordDAO.ChecklistRow;
import com.hospital.vaccination.service.AuthService;
import com.hospital.vaccination.ui.components.EmptyState;
import com.hospital.vaccination.ui.components.PageHeader;
import com.hospital.vaccination.ui.components.PremiumTable;
import com.hospital.vaccination.ui.components.RoundedButton;
import com.hospital.vaccination.ui.components.StatCard;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Planning view for nurses.
 *
 * <p>Shows only future PENDING doses from tomorrow onward. Multiple doses for
 * the same child on the same date are grouped into one row for a cleaner,
 * easier-to-read schedule. Individual doses remain separate in the database
 * and in Today's Checklist, where nurses update each status separately.
 */
public class UpcomingChecklistPanel extends JPanel {

    private static final DateTimeFormatter AD_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final VaccinationRecordDAO dao = new VaccinationRecordDAO();
    private final UpcomingTableModel model = new UpcomingTableModel();
    private final PremiumTable table = new PremiumTable(model);

    private final StatCard cardDoses = new StatCard(
            "Upcoming doses", "0", Icons.CALENDAR_DAY, UI.PRIMARY);
    private final StatCard cardVisits = new StatCard(
            "Clinic visits", "0", Icons.USERS, UI.SUCCESS);
    private final StatCard cardNearest = new StatCard(
            "Nearest due", "—", Icons.CALENDAR, UI.WARN);

    private JPanel tableCard;

    public UpcomingChecklistPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG_APP);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        int[] widths = {40, 120, 115, 155, 255, 65, 130, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.setRowHeight(40);
        table.getColumnModel().getColumn(1).setCellRenderer(new AdDateRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getRowSorter().setSortKeys(List.of(
                new RowSorter.SortKey(1, SortOrder.ASCENDING)));

        refresh();
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader(
                "Upcoming Vaccination Checklist",
                "Future pending doses from tomorrow onward — grouped by child and due date.");

        RoundedButton refresh = new RoundedButton(
                "Refresh", RoundedButton.Style.SECONDARY).withIcon(Icons.REFRESH);
        refresh.addActionListener(e -> refresh());
        header.addAction(refresh);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(UI.BG_APP);
        body.setBorder(UI.padding(24, 32));

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(cardDoses);
        stats.add(cardVisits);
        stats.add(cardNearest);
        body.add(stats, BorderLayout.NORTH);

        tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UI.CARD_BG);
        tableCard.setBorder(BorderFactory.createLineBorder(UI.BORDER, 1, true));
        body.add(tableCard, BorderLayout.CENTER);

        return body;
    }

    /** Refresh this page from the dashboard's F5 shortcut if needed. */
    public void refreshExternally() {
        refresh();
    }

    private void refresh() {
        AuthService.touch();

        try {
            // due_date > today: today's doses never appear on this planning page.
            List<ChecklistRow> individualRecords =
                    dao.findUpcomingChecklist(LocalDate.now());

            List<UpcomingGroup> groupedRows = groupByChildAndDueDate(individualRecords);
            model.setRows(groupedRows);
            table.getRowSorter().setSortKeys(List.of(
                    new RowSorter.SortKey(1, SortOrder.ASCENDING)));
            Object nurse;
            updateStats(groupedRows);
            updateTableView(groupedRows);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            showError("Could not load upcoming vaccinations: " + ex.getMessage());
        }
    }

    /**
     * Keeps every dose in the database but combines records with the same
     * child ID and due date into one UI row.
     */
    private List<UpcomingGroup> groupByChildAndDueDate(List<ChecklistRow> records) {
        Map<GroupKey, List<ChecklistRow>> grouped = new LinkedHashMap<>();

        for (ChecklistRow row : records) {
            GroupKey key = new GroupKey(
                    row.record().getChildId(),
                    row.record().getDueDate());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }

        List<UpcomingGroup> result = new ArrayList<>();
        for (List<ChecklistRow> sameVisit : grouped.values()) {
            ChecklistRow first = sameVisit.get(0);
            result.add(new UpcomingGroup(
                    first.record().getDueDate(),
                    first.childName(),
                    first.parentPhone(),
                    List.copyOf(sameVisit)));
        }
        return result;
    }

    private void updateStats(List<UpcomingGroup> groups) {
        int totalDoses = groups.stream().mapToInt(UpcomingGroup::doseCount).sum();
        cardDoses.setValue(String.valueOf(totalDoses));
        cardVisits.setValue(String.valueOf(groups.size()));

        if (groups.isEmpty()) {
            cardNearest.setValue("—");
            cardNearest.setSubtitle("No future pending doses");
            return;
        }

        LocalDate nearest = groups.get(0).dueDate();
        cardNearest.setValue(AD_DATE.format(nearest));
        cardNearest.setSubtitle(daysLeft(nearest));
    }

    private void updateTableView(List<UpcomingGroup> groups) {
        tableCard.removeAll();

        if (groups.isEmpty()) {
            tableCard.add(new EmptyState(
                            Icons.CALENDAR,
                            "No upcoming vaccinations",
                            "There are no pending vaccine doses after today."),
                    BorderLayout.CENTER);
        } else {
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Color.WHITE);
            tableCard.add(scroll, BorderLayout.CENTER);
        }

        tableCard.revalidate();
        tableCard.repaint();
    }

    private void showError(String message) {
        tableCard.removeAll();
        tableCard.add(new EmptyState(
                Icons.TRIANGLE_WARN,
                "Unable to load upcoming checklist",
                message), BorderLayout.CENTER);
        tableCard.revalidate();
        tableCard.repaint();
    }

    private static String daysLeft(LocalDate dueDate) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (days == 1) return "Tomorrow";
        return days + " days away";
    }

    // ---------------------------------------------------------------------

    private record GroupKey(int childId, LocalDate dueDate) { }

    /** One upcoming clinic visit: a child, one date, and one or more doses. */
    private record UpcomingGroup(LocalDate dueDate,
                                 String childName,
                                 String parentPhone,
                                 List<ChecklistRow> doses) {

        int doseCount() {
            return doses.size();
        }

        String vaccineNames() {
            return doses.stream()
                    .map(row -> row.record().getVaccineName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }
    }

    private static class UpcomingTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "#", "Due date (AD)", "Due date (BS)", "Child name",
                "Vaccines due", "Doses", "Parent phone", "In"
        };

        private List<UpcomingGroup> rows = new ArrayList<>();

        void setRows(List<UpcomingGroup> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 || columnIndex == 5 ? Integer.class
                    : columnIndex == 1 ? LocalDate.class
                      : String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UpcomingGroup group = rows.get(rowIndex);
            LocalDate due = group.dueDate();

            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> due;
                case 2 -> BSDateConverter.toBS(due).format();
                case 3 -> group.childName();
                case 4 -> group.vaccineNames();
                case 5 -> group.doseCount();
                case 6 -> group.parentPhone();
                case 7 -> daysLeft(due);
                default -> "";
            };
        }
    }

    /** Displays LocalDate nicely while keeping chronological date sorting. */
    private static class AdDateRenderer extends DefaultTableCellRenderer {
        AdDateRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {

            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setText(value instanceof LocalDate date ? AD_DATE.format(date) : "");
            if (!selected) {
                setBackground(row % 2 == 0 ? Color.WHITE : UI.ROW_ALT);
                setForeground(UI.TEXT_PRIMARY);
            }
            return this;
        }
    }
}
