package com.hospital.vaccination.ui;

import com.hospital.vaccination.dao.ChildDAO;
import com.hospital.vaccination.dao.ChildDAO.ChildRow;
import com.hospital.vaccination.dao.VaccinationRecordDAO;
import com.hospital.vaccination.model.Child;
import com.hospital.vaccination.model.VaccinationRecord;
import com.hospital.vaccination.ui.components.*;
import com.hospital.vaccination.ui.components.RoundedButton.Style;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.DateUtil;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Shared staff page: browse every registered child. */
public class AllChildrenPanel extends JPanel {

    private final ChildDAO             childDAO   = new ChildDAO();
    private final VaccinationRecordDAO recordDAO  = new VaccinationRecordDAO();

    private final RoundedTextField searchField = new RoundedTextField(24);
    private final ChildrenTableModel model = new ChildrenTableModel();
    private final PremiumTable table = new PremiumTable(model);

    private final StatCard cardTotal = new StatCard("Total children",     "0", Icons.CHILD,       UI.PRIMARY);
    private final StatCard cardMonth = new StatCard("New this month",     "0", Icons.CHART_LINE,  UI.SUCCESS);
    private final StatCard cardShown = new StatCard("Currently shown",    "0", Icons.EYE,         UI.WARN);

    public AllChildrenPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG_APP);
        searchField.setPlaceholder("Search by name or phone…");

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        int[] w = { 180, 100, 100, 80, 160, 120, 160, 140 };
        for (int i = 0; i < w.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        refresh();
    }

    private PageHeader buildHeader() {
        PageHeader h = new PageHeader("All Registered Children",
                "Search, view full vaccination history, and reprint cards.");
        RoundedButton refresh = new RoundedButton("Refresh", Style.SECONDARY).withIcon(Icons.REFRESH);
        refresh.addActionListener(e -> refresh());
        RoundedButton history = new RoundedButton("View history", Style.SECONDARY).withIcon(Icons.EYE);
        history.addActionListener(e -> onViewHistory());
        RoundedButton reprint = new RoundedButton("Reprint card", Style.PRIMARY).withIcon(Icons.PRINT);
        reprint.addActionListener(e -> onReprint());
        h.addAction(refresh); h.addAction(history); h.addAction(reprint);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(UI.BG_APP);
        body.setBorder(UI.padding(24, 32));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setOpaque(false);
        JLabel icon = new JLabel(Icons.SEARCH);
        icon.setFont(UI.icon(14f));
        icon.setForeground(UI.TEXT_SECONDARY);
        searchRow.add(icon);
        searchField.setPreferredSize(new Dimension(320, 36));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate (DocumentEvent e) { refresh(); }
            @Override public void removeUpdate (DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        searchRow.add(searchField);
        top.add(searchRow, BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(cardTotal); stats.add(cardMonth); stats.add(cardShown);
        top.add(stats, BorderLayout.SOUTH);
        body.add(top, BorderLayout.NORTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UI.CARD_BG);
        wrap.setBorder(BorderFactory.createLineBorder(UI.BORDER, 1, true));
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        wrap.add(sp, BorderLayout.CENTER);
        body.add(wrap, BorderLayout.CENTER);
        return body;
    }

    public void refreshExternally() { refresh(); }
    public void focusSearch()       { searchField.requestFocusInWindow(); searchField.selectAll(); }

    private void refresh() {
        try {
            List<ChildRow> shown = childDAO.findAllForAdmin(searchField.getText());
            List<ChildRow> all   = childDAO.findAllForAdmin(null);
            model.setRows(shown);
            YearMonth thisMonth = YearMonth.now();
            long m = all.stream()
                    .filter(r -> r.child().getRegisteredAt() != null)
                    .filter(r -> YearMonth.from(r.child().getRegisteredAt()).equals(thisMonth))
                    .count();
            cardTotal.setValue(String.valueOf(all.size()));
            cardMonth.setValue(String.valueOf(m));
            cardShown.setValue(String.valueOf(shown.size()));
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Could not load: " + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Child selectedChild() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return model.rows.get(table.convertRowIndexToModel(viewRow)).child();
    }

    private void onViewHistory() {
        Child c = selectedChild();
        if (c == null) { JOptionPane.showMessageDialog(this, "Select a child first.", "No selection", JOptionPane.WARNING_MESSAGE); return; }
        List<VaccinationRecord> records = recordDAO.findByChild(c.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:540px;font-family:Segoe UI;'>");
        sb.append("<b style='font-size:14pt;color:#0F4C81;'>").append(escape(c.getName())).append("</b><br>");
        sb.append("<span style='color:#687486;'>DOB: ").append(DateUtil.display(c.getDateOfBirth()))
                .append(" &middot; ").append(BSDateConverter.toBS(c.getDateOfBirth()).display())
                .append(" BS</span><br>");
        sb.append("<span style='color:#687486;'>Parent: ").append(escape(c.getParentName()))
                .append(" &middot; ").append(escape(c.getParentPhone())).append("</span><br><br>");

        sb.append("<table cellpadding='6' cellspacing='0' border='0' " +
                "style='border-collapse:collapse;border:1px solid #DFE4EA;'>");
        sb.append("<tr bgcolor='#0F1E3D' style='color:white;'>" +
                "<th align='left'>VACCINE</th>" +
                "<th align='left'>DUE (AD)</th>" +
                "<th align='left'>DUE (BS)</th>" +
                "<th align='left'>STATUS</th>" +
                "<th align='left'>GIVEN</th>" +
                "<th align='left'>SMS</th></tr>");
        boolean zebra = false;
        for (VaccinationRecord r : records) {
            String bg = zebra ? " bgcolor='#F9FBFD'" : "";
            zebra = !zebra;
            String col = switch (r.getStatus()) {
                case COMPLETED -> "#00A86B";
                case MISSED    -> "#E63946";
                default        -> "#FFA827";
            };
            sb.append("<tr").append(bg).append(">")
                    .append("<td><b>").append(r.getVaccineName()).append("</b></td>")
                    .append("<td>").append(DateUtil.display(r.getDueDate())).append("</td>")
                    .append("<td>").append(BSDateConverter.toBS(r.getDueDate()).format()).append("</td>")
                    .append("<td><b style='color:").append(col).append(";'>&#9679; ").append(r.getStatus().name()).append("</b></td>")
                    .append("<td>").append(r.getCompletedDate() == null ? "&mdash;" : DateUtil.display(r.getCompletedDate())).append("</td>")
                    .append("<td align='center'>").append(r.getReminderCount()).append("</td>")
                    .append("</tr>");
        }
        sb.append("</table></body></html>");

        JLabel content = new JLabel(sb.toString());
        JScrollPane scroll = new JScrollPane(content);
        scroll.setPreferredSize(new Dimension(720, 500));
        JOptionPane.showMessageDialog(this, scroll,
                "Vaccination history — " + c.getName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void onReprint() {
        Child c = selectedChild();
        if (c == null) { JOptionPane.showMessageDialog(this, "Select a child first.", "No selection", JOptionPane.WARNING_MESSAGE); return; }
        List<VaccinationRecord> records = recordDAO.findByChild(c.getId());
        CardActionDialog.show(this, c, records,
                "Vaccination card — " + c.getName(), null);
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    // ---- model --------------------------------------------------------------

    private static class ChildrenTableModel extends AbstractTableModel {
        private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private final String[] COLS = {
                "Name", "DOB (AD)", "DOB (BS)", "Gender",
                "Parent", "Phone", "Registered by", "Registered at"
        };
        List<ChildRow> rows = new ArrayList<>();
        void setRows(List<ChildRow> rows) { this.rows = rows; fireTableDataChanged(); }
        @Override public int getRowCount()           { return rows.size(); }
        @Override public int getColumnCount()        { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public Object getValueAt(int r, int c) {
            Child ch = rows.get(r).child();
            return switch (c) {
                case 0 -> ch.getName();
                case 1 -> DateUtil.display(ch.getDateOfBirth());
                case 2 -> BSDateConverter.toBS(ch.getDateOfBirth()).format();
                case 3 -> ch.getGender().name();
                case 4 -> ch.getParentName();
                case 5 -> ch.getParentPhone();
                case 6 -> rows.get(r).nurseName();
                case 7 -> ch.getRegisteredAt() == null ? "" : TS.format(ch.getRegisteredAt());
                default -> "";
            };
        }
    }
}
