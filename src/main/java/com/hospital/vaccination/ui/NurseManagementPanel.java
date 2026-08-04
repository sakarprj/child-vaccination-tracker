package com.hospital.vaccination.ui;

import com.hospital.vaccination.dao.UserDAO;
import com.hospital.vaccination.model.User;
import com.hospital.vaccination.ui.components.PageHeader;
import com.hospital.vaccination.ui.components.PremiumTable;
import com.hospital.vaccination.ui.components.RoundedButton;
import com.hospital.vaccination.ui.components.RoundedButton.Style;
import com.hospital.vaccination.ui.components.RoundedTextField;
import com.hospital.vaccination.ui.components.StatCard;
import com.hospital.vaccination.ui.components.StatusPill;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.PasswordUtil;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NurseManagementPanel extends JPanel {

    private final UserDAO dao = new UserDAO();
    private final NurseTableModel model = new NurseTableModel();
    private final PremiumTable table = new PremiumTable(model);

    private final StatCard cardTotal    = new StatCard("Total accounts", "0", Icons.USERS,        UI.PRIMARY);
    private final StatCard cardOnline   = new StatCard("Online now",     "0", Icons.CIRCLE_CHECK, UI.SUCCESS);
    private final StatCard cardInactive = new StatCard("Deactivated",    "0", Icons.CIRCLE_XMARK, UI.DANGER);

    /** Live-refresh timer for the Presence column. */
    private final Timer presenceTimer;

    public NurseManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG_APP);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        table.useStatusPillOn(3);   // Account status
        table.getColumnModel().getColumn(4).setCellRenderer(new PresencePillRenderer()); // Presence

        int[] w = { 120, 200, 90, 110, 110, 160 };
        for (int i = 0; i < w.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        refresh();

        // Refresh the table every 30 seconds so the Presence column stays live
        presenceTimer = new Timer(30_000, e -> refresh());
        presenceTimer.setRepeats(true);
        presenceTimer.start();
    }

    private PageHeader buildHeader() {
        PageHeader h = new PageHeader("Nurse Accounts",
                "Add new staff, reset passwords, and deactivate accounts. Presence updates every 30 seconds.");
        RoundedButton reset = new RoundedButton("Reset password", Style.SECONDARY).withIcon(Icons.KEY);
        reset.addActionListener(e -> onResetPassword());
        RoundedButton deac = new RoundedButton("Deactivate", Style.SECONDARY).withIcon(Icons.TRASH);
        deac.addActionListener(e -> onDeactivate());
        RoundedButton refresh = new RoundedButton("Refresh", Style.SECONDARY).withIcon(Icons.REFRESH);
        refresh.addActionListener(e -> refresh());
        RoundedButton add = new RoundedButton("Add nurse", Style.PRIMARY).withIcon(Icons.USER_PLUS);
        add.addActionListener(e -> onAdd());
        h.addAction(reset); h.addAction(deac); h.addAction(refresh); h.addAction(add);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(UI.BG_APP);
        body.setBorder(UI.padding(24, 32));

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(cardTotal); stats.add(cardOnline); stats.add(cardInactive);
        body.add(stats, BorderLayout.NORTH);

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

    private void refresh() {
        // The admin viewing this page counts as being "online"
        com.hospital.vaccination.service.AuthService.touch();

        List<User> all = dao.findAll();
        model.setRows(all);
        long online   = all.stream().filter(User::isActive)
                .filter(u -> u.presence() == User.Presence.ONLINE).count();
        long inactive = all.stream().filter(u -> !u.isActive()).count();
        cardTotal   .setValue(String.valueOf(all.size()));
        cardOnline  .setValue(String.valueOf(online));
        cardInactive.setValue(String.valueOf(inactive));
    }

    // ---- actions ------------------------------------------------------------

    private void onAdd() {
        RoundedTextField userF = new RoundedTextField(15);
        RoundedTextField nameF = new RoundedTextField(15);
        JPasswordField passF = new JPasswordField(15);
        RoundedTextField.styleAsRounded(passF);
        JComboBox<User.Role> roleBox = new JComboBox<>(User.Role.values());
        roleBox.setSelectedItem(User.Role.NURSE);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Username:"));    form.add(userF);
        form.add(new JLabel("Full name:"));   form.add(nameF);
        form.add(new JLabel("Password:"));    form.add(passF);
        form.add(new JLabel("Role:"));        form.add(roleBox);

        int r = JOptionPane.showConfirmDialog(this, form,
                "Add new account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String username = userF.getText().trim();
        String fullName = nameF.getText().trim();
        String password = new String(passF.getPassword());
        if (username.isEmpty() || fullName.isEmpty() || password.length() < 4) {
            err("Username and full name required; password >= 4 characters."); return;
        }
        if (dao.usernameExists(username)) { err("That username is already taken."); return; }

        User u = new User();
        u.setUsername(username); u.setFullName(fullName);
        u.setRole((User.Role) roleBox.getSelectedItem());
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setActive(true);
        try { dao.insert(u); info("Account created for " + fullName + "."); refresh(); }
        catch (RuntimeException ex) { ex.printStackTrace(); err("Could not create: " + ex.getMessage()); }
    }

    private void onResetPassword() {
        User sel = selectedUser();
        if (sel == null) { err("Select a row first."); return; }
        JPasswordField p1 = new JPasswordField(15);
        JPasswordField p2 = new JPasswordField(15);
        RoundedTextField.styleAsRounded(p1);
        RoundedTextField.styleAsRounded(p2);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("New password:")); form.add(p1);
        form.add(new JLabel("Confirm:"));      form.add(p2);
        int r = JOptionPane.showConfirmDialog(this, form,
                "Reset password for " + sel.getUsername(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        String a = new String(p1.getPassword()); String b = new String(p2.getPassword());
        if (a.length() < 4) { err("Password must be at least 4 characters."); return; }
        if (!a.equals(b))   { err("Passwords do not match."); return; }
        dao.updatePasswordHash(sel.getId(), PasswordUtil.hash(a));
        info("Password reset for " + sel.getUsername() + ".");
    }

    private void onDeactivate() {
        User sel = selectedUser();
        if (sel == null) { err("Select a row first."); return; }
        if (!sel.isActive()) { err(sel.getUsername() + " is already inactive."); return; }
        if (sel.getRole() == User.Role.ADMIN) { err("Refusing to deactivate an ADMIN account from here."); return; }
        int r = JOptionPane.showConfirmDialog(this,
                "Deactivate " + sel.getFullName() + " (" + sel.getUsername() + ")?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        dao.deactivate(sel.getId());
        refresh();
    }

    private User selectedUser() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return model.rows.get(table.convertRowIndexToModel(viewRow));
    }
    private void err(String m)  { JOptionPane.showMessageDialog(this, m, "Cannot proceed", JOptionPane.WARNING_MESSAGE); }
    private void info(String m) { JOptionPane.showMessageDialog(this, m, "Done", JOptionPane.INFORMATION_MESSAGE); }

    // ---- table model ------------------------------------------------------

    private static class NurseTableModel extends AbstractTableModel {
        private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm 'today'");
        private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM HH:mm");
        private final String[] COLS = { "Username", "Full name", "Role", "Account", "Presence", "Last active" };
        List<User> rows = new ArrayList<>();
        void setRows(List<User> rows) { this.rows = rows; fireTableDataChanged(); }
        @Override public int getRowCount()           { return rows.size(); }
        @Override public int getColumnCount()        { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public Object getValueAt(int r, int c) {
            User u = rows.get(r);
            return switch (c) {
                case 0 -> u.getUsername();
                case 1 -> u.getFullName();
                case 2 -> u.getRole().name();
                case 3 -> u.isActive() ? "ACTIVE" : "INACTIVE";
                case 4 -> u.presence().name();
                case 5 -> {
                    if (u.getLastActiveAt() == null) yield "Never";
                    java.time.LocalDate today = java.time.LocalDate.now();
                    if (u.getLastActiveAt().toLocalDate().equals(today)) {
                        yield TS.format(u.getLastActiveAt());
                    }
                    yield DATE.format(u.getLastActiveAt());
                }
                default -> "";
            };
        }
    }

    /** Custom renderer for the Presence column — different colors than Account status. */
    private static class PresencePillRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                                                       boolean sel, boolean focus, int row, int col) {
            String s = v == null ? "OFFLINE" : v.toString().toUpperCase();
            Color fg, bg;
            switch (s) {
                case "ONLINE"  -> { fg = UI.SUCCESS; bg = UI.SUCCESS_BG; }
                case "IDLE"    -> { fg = UI.WARN;    bg = UI.WARN_BG; }
                default        -> { fg = UI.TEXT_MUTED; bg = new Color(235, 238, 242); }
            }
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrap.setBackground(sel ? UI.PRIMARY_LIGHT
                    : (row % 2 == 0 ? Color.WHITE : UI.ROW_ALT));
            wrap.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            StatusPill pill = new StatusPill(s, fg, bg);
            pill.setPreferredSize(new Dimension(100, 22));
            wrap.add(pill);
            return wrap;
        }
    }
}
