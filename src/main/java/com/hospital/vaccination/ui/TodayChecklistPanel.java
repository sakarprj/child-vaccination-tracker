package com.hospital.vaccination.ui;

import com.hospital.vaccination.dao.VaccinationRecordDAO;
import com.hospital.vaccination.dao.VaccinationRecordDAO.ChecklistRow;
import com.hospital.vaccination.model.VaccinationRecord;
import com.hospital.vaccination.ui.components.*;
import com.hospital.vaccination.ui.components.RoundedButton.Style;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.BSDateConverter.BSDate;
import com.hospital.vaccination.util.DateUtil;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Today's checklist — premium styled. */
public class TodayChecklistPanel extends JPanel {

    private final VaccinationRecordDAO dao = new VaccinationRecordDAO();
    private LocalDate selectedDate = LocalDate.now();

    private final RoundedTextField bsField = new RoundedTextField(11);
    private final RoundedTextField adField = new RoundedTextField(11);
    private boolean syncing = false;

    private final ChecklistTableModel model = new ChecklistTableModel();
    private final PremiumTable table = new PremiumTable(model);

    private final StatCard cardTotal     = new StatCard("Total due",   "0", Icons.CALENDAR_DAY, UI.PRIMARY);
    private final StatCard cardPending   = new StatCard("Pending",     "0", Icons.CIRCLE_CLOCK, UI.WARN);
    private final StatCard cardCompleted = new StatCard("Completed",   "0", Icons.CIRCLE_CHECK, UI.SUCCESS);
    private final StatCard cardMissed    = new StatCard("Missed",      "0", Icons.CIRCLE_XMARK, UI.DANGER);

    private final JLabel dateBanner = new JLabel();

    public TodayChecklistPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG_APP);

        bsField.setPlaceholder("YYYY-MM-DD");
        adField.setPlaceholder("YYYY-MM-DD");

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        LocalDate today = LocalDate.now();
        syncingSet(adField, today.toString());
        syncingSet(bsField, BSDateConverter.toBS(today).format());
        wireDateSync();

        table.useStatusPillOn(5);
        int[] widths = { 40, 200, 100, 110, 130, 130, 60 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        refresh();
    }

    private PageHeader buildHeader() {
        PageHeader h = new PageHeader("Today's Vaccination Checklist",
                "Tick each dose as it's administered — status saves automatically.");

        RoundedButton todayBtn = new RoundedButton("Today", Style.SECONDARY).withIcon(Icons.CALENDAR_DAY);
        todayBtn.addActionListener(e -> {
            LocalDate now = LocalDate.now();
            syncingSet(adField, now.toString());
            syncingSet(bsField, BSDateConverter.toBS(now).format());
            selectedDate = now;
            refresh();
        });
        RoundedButton refresh = new RoundedButton("Refresh", Style.SECONDARY).withIcon(Icons.REFRESH);
        refresh.addActionListener(e -> refresh());

        h.addAction(todayBtn);
        h.addAction(refresh);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(UI.BG_APP);
        body.setBorder(UI.padding(24, 32));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(buildDatePicker(), BorderLayout.NORTH);
        top.add(buildStats(), BorderLayout.CENTER);
        body.add(top, BorderLayout.NORTH);

        body.add(buildTableCard(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildDatePicker() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);

        JLabel prompt = new JLabel("Viewing schedule for");
        prompt.setFont(UI.body());
        prompt.setForeground(UI.TEXT_SECONDARY);
        row.add(prompt);
        row.add(Box.createHorizontalStrut(4));

        JLabel bs = new JLabel("BS");
        bs.setFont(UI.bold(11.5f));
        bs.setForeground(UI.TEXT_SECONDARY);
        row.add(bs);
        row.add(bsField);
        row.add(Box.createHorizontalStrut(10));
        JLabel ad = new JLabel("AD");
        ad.setFont(UI.bold(11.5f));
        ad.setForeground(UI.TEXT_SECONDARY);
        row.add(ad);
        row.add(adField);

        RoundedButton go = new RoundedButton("Show", Style.PRIMARY);
        go.addActionListener(e -> onDateChanged());
        row.add(go);

        wrap.add(row, BorderLayout.WEST);

        dateBanner.setFont(UI.bold(13f));
        dateBanner.setForeground(UI.PRIMARY);
        dateBanner.setBorder(UI.padding(6, 0));
        wrap.add(dateBanner, BorderLayout.EAST);

        return wrap;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.add(cardTotal);
        row.add(cardPending);
        row.add(cardCompleted);
        row.add(cardMissed);
        return row;
    }

    private JPanel tableCard;

    private JPanel buildTableCard() {
        tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UI.CARD_BG);
        tableCard.setBorder(BorderFactory.createLineBorder(UI.BORDER, 1, true));
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        tableCard.add(sp, BorderLayout.CENTER);
        return tableCard;
    }

    // ---- BS <-> AD sync ----------------------------------------------------

    private void wireDateSync() {
        bsField.getDocument().addDocumentListener(new SimpleDocListener(this::onBsTyped));
        adField.getDocument().addDocumentListener(new SimpleDocListener(this::onAdTyped));
    }

    private void onBsTyped() {
        if (syncing) return;
        String t = bsField.getText().trim();
        if (t.isEmpty()) { syncingSet(adField, ""); return; }
        BSDate bs = BSDateConverter.parse(t);
        if (bs == null) return;
        try {
            LocalDate ad = BSDateConverter.toAD(bs.year(), bs.month(), bs.day());
            syncingSet(adField, ad.toString());
        } catch (Exception ignored) { }
    }
    private void onAdTyped() {
        if (syncing) return;
        String t = adField.getText().trim();
        if (t.isEmpty()) { syncingSet(bsField, ""); return; }
        LocalDate ad = DateUtil.parse(t);
        if (ad == null) return;
        try { syncingSet(bsField, BSDateConverter.toBS(ad).format()); }
        catch (Exception ignored) { }
    }
    private void syncingSet(JTextField f, String text) {
        syncing = true;
        try { f.setText(text); } finally { syncing = false; }
    }

    private void onDateChanged() {
        LocalDate d = DateUtil.parse(adField.getText());
        if (d == null) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid BS or AD date (yyyy-mm-dd).",
                    "Invalid date", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectedDate = d;
        refresh();
    }

    // ---- refresh -----------------------------------------------------------

    public void refreshExternally() { refresh(); }

    private void refresh() {
        // Nurse is actively using the app — update their presence
        com.hospital.vaccination.service.AuthService.touch();

        List<ChecklistRow> rows;
        try { rows = dao.findChecklistFor(selectedDate); }
        catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Could not load: " + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        model.setRows(rows);
        refreshCounts();

        BSDate bs = BSDateConverter.toBS(selectedDate);
        String prefix = selectedDate.equals(LocalDate.now()) ? "Today  \u2022  " : "";
        dateBanner.setText(prefix + DateUtil.display(selectedDate) + "   /   " + bs.display() + " BS");

        // Empty state swap
        if (rows.isEmpty()) {
            tableCard.removeAll();
            tableCard.add(new EmptyState(Icons.INBOX,
                            "No vaccinations due on this date",
                            "Nothing to do — try picking a different date."),
                    BorderLayout.CENTER);
        } else {
            tableCard.removeAll();
            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(null);
            sp.getViewport().setBackground(Color.WHITE);
            tableCard.add(sp, BorderLayout.CENTER);
        }
        tableCard.revalidate();
        tableCard.repaint();
    }

    private void refreshCounts() {
        List<ChecklistRow> rows = model.rows;
        long pending   = rows.stream().filter(r -> r.record().getStatus() == VaccinationRecord.Status.PENDING).count();
        long completed = rows.stream().filter(r -> r.record().getStatus() == VaccinationRecord.Status.COMPLETED).count();
        long missed    = rows.stream().filter(r -> r.record().getStatus() == VaccinationRecord.Status.MISSED).count();
        cardTotal    .setValue(String.valueOf(rows.size()));
        cardPending  .setValue(String.valueOf(pending));
        cardCompleted.setValue(String.valueOf(completed));
        cardMissed   .setValue(String.valueOf(missed));
    }

    // ---- table model -------------------------------------------------------

    private class ChecklistTableModel extends AbstractTableModel {
        private final String[] COLS = {
                "#", "Child name", "Age (today)", "Vaccine", "Parent phone", "Status", "\u2713"
        };
        private final Class<?>[] TYPES = {
                Integer.class, String.class, String.class, String.class,
                String.class, String.class, Boolean.class
        };
        List<ChecklistRow> rows = new ArrayList<>();

        void setRows(List<ChecklistRow> rows) { this.rows = rows; fireTableDataChanged(); }
        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public Class<?> getColumnClass(int c) { return TYPES[c]; }
        @Override public boolean isCellEditable(int r, int c) {
            if (c != 6) return false;
            return rows.get(r).record().getStatus() != VaccinationRecord.Status.MISSED;
        }
        @Override public Object getValueAt(int r, int c) {
            ChecklistRow row = rows.get(r);
            VaccinationRecord rec = row.record();
            return switch (c) {
                case 0 -> r + 1;
                case 1 -> row.childName();
                case 2 -> ageString(row.childDob(), LocalDate.now());
                case 3 -> rec.getVaccineName();
                case 4 -> row.parentPhone();
                case 5 -> rec.getStatus().name();
                case 6 -> rec.getStatus() == VaccinationRecord.Status.COMPLETED;
                default -> "";
            };
        }
        @Override public void setValueAt(Object value, int r, int c) {
            if (c != 6) return;
            boolean checked = Boolean.TRUE.equals(value);
            ChecklistRow row = rows.get(r);
            VaccinationRecord rec = row.record();
            try {
                if (checked) {
                    dao.markCompleted(rec.getId(), LocalDate.now());
                    rec.setStatus(VaccinationRecord.Status.COMPLETED);
                    rec.setCompletedDate(LocalDate.now());
                } else {
                    dao.markPending(rec.getId());
                    rec.setStatus(VaccinationRecord.Status.PENDING);
                    rec.setCompletedDate(null);
                }
                com.hospital.vaccination.service.AuthService.touch();
                fireTableRowsUpdated(r, r);
                refreshCounts();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(TodayChecklistPanel.this,
                        "Could not update: " + ex.getMessage(),
                        "Database error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String ageString(LocalDate dob, LocalDate on) {
        if (dob == null) return "";
        long days = ChronoUnit.DAYS.between(dob, on);
        if (days < 0)   return "not born";
        if (days == 0)  return "newborn";
        if (days < 60)  return (days / 7) + " weeks";
        Period p = Period.between(dob, on);
        if (p.getYears() > 0)  return p.getYears() + " yr " + p.getMonths() + " mo";
        return p.getMonths() + " months";
    }

    private static class SimpleDocListener implements DocumentListener {
        private final Runnable r;
        SimpleDocListener(Runnable r) { this.r = r; }
        @Override public void insertUpdate (DocumentEvent e) { r.run(); }
        @Override public void removeUpdate (DocumentEvent e) { r.run(); }
        @Override public void changedUpdate(DocumentEvent e) { r.run(); }
    }
}
