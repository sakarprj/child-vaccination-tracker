package com.hospital.vaccination.ui;

import com.hospital.vaccination.model.Child;
import com.hospital.vaccination.model.User;
import com.hospital.vaccination.service.AuthService;
import com.hospital.vaccination.service.ChildRegistrationService;
import com.hospital.vaccination.ui.components.PageHeader;
import com.hospital.vaccination.ui.components.RoundedButton;
import com.hospital.vaccination.ui.components.RoundedButton.Style;
import com.hospital.vaccination.ui.components.RoundedTextField;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.BSDateConverter.BSDate;
import com.hospital.vaccination.util.DateUtil;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;

public class ChildRegistrationPanel extends JPanel {

    private final RoundedTextField nameField        = new RoundedTextField(22);
    private final RoundedTextField dobBsField       = new RoundedTextField(11);
    private final RoundedTextField dobAdField       = new RoundedTextField(11);
    private final JLabel           dobHint          = new JLabel(" ");
    private final JComboBox<Child.Gender> genderBox = new JComboBox<>(Child.Gender.values());
    private final RoundedTextField parentNameField  = new RoundedTextField(22);
    private final RoundedTextField parentPhoneField = new RoundedTextField(22);
    private final JTextArea        addressArea      = new JTextArea(3, 22);

    private final ChildRegistrationService service = new ChildRegistrationService();
    private boolean syncing = false;

    public ChildRegistrationPanel() {
        setLayout(new BorderLayout());
        setBackground(UI.BG_APP);

        nameField.setPlaceholder("e.g. Aarav Sharma");
        parentNameField.setPlaceholder("Parent's full name");
        parentPhoneField.setPlaceholder("98XXXXXXXX");
        dobBsField.setPlaceholder("YYYY-MM-DD");
        dobAdField.setPlaceholder("YYYY-MM-DD");

        RoundedTextField.styleAsRounded(addressArea);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        wireDobSync();
    }

    private PageHeader buildHeader() {
        PageHeader h = new PageHeader("Register New Child",
                "Add a child and auto-generate the full Nepal NIP vaccination schedule.");

        RoundedButton clear = new RoundedButton("Clear", Style.SECONDARY).withIcon(Icons.XMARK);
        clear.addActionListener(e -> clearForm());

        RoundedButton save = new RoundedButton("Register & generate", Style.PRIMARY).withIcon(Icons.PLUS);
        save.addActionListener(e -> onSave());

        h.addAction(clear);
        h.addAction(save);
        return h;
    }

    private JScrollPane buildBody() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UI.BG_APP);
        outer.setBorder(UI.padding(28, 32));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UI.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UI.BORDER, 1, true),
                UI.padding(28)));

        JLabel section = new JLabel("Child & parent details");
        section.setFont(UI.h3());
        section.setForeground(UI.TEXT_PRIMARY);
        section.setBorder(UI.padding(0, 0, 16, 0));
        card.add(section, BorderLayout.NORTH);
        card.add(buildForm(), BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        outer.add(card, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        outer.add(Box.createGlue(), gbc);

        JScrollPane sp = new JScrollPane(outer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(UI.BG_APP);
        sp.setBackground(UI.BG_APP);
        return sp;
    }

    private JPanel buildForm() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 0.5;

        int y = 0;
        g.gridx = 0; g.gridy = y;
        grid.add(labeled("Child's name *", nameField), g);
        g.gridx = 1;
        grid.add(labeled("Gender *", genderBox), g);
        y++;

        g.gridx = 0; g.gridy = y; g.gridwidth = 2;
        grid.add(labeled("Date of birth *", dobRow()), g);
        y++;

        g.gridx = 0; g.gridy = y; g.gridwidth = 2;
        dobHint.setFont(UI.small().deriveFont(Font.ITALIC));
        dobHint.setForeground(UI.TEXT_SECONDARY);
        dobHint.setBorder(UI.padding(0, 10, 10, 10));
        grid.add(dobHint, g);
        y++;

        g.gridwidth = 1;
        g.gridx = 0; g.gridy = y;
        grid.add(labeled("Parent's name *", parentNameField), g);
        g.gridx = 1;
        grid.add(labeled("Parent's phone *", parentPhoneField), g);
        y++;

        g.gridx = 0; g.gridy = y; g.gridwidth = 2;
        JScrollPane addrScroll = new JScrollPane(addressArea);
        addrScroll.setPreferredSize(new Dimension(0, 76));
        addrScroll.setBorder(BorderFactory.createLineBorder(UI.BORDER, 1, true));
        grid.add(labeled("Address", addrScroll), g);

        return grid;
    }

    private JPanel dobRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(fieldLabel("BS"));
        row.add(dobBsField);
        row.add(Box.createHorizontalStrut(12));
        row.add(fieldLabel("AD"));
        row.add(dobAdField);
        return row;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UI.bold(11.5f));
        l.setForeground(UI.TEXT_SECONDARY);
        return l;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(UI.bold(12f));
        l.setForeground(UI.TEXT_PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(UI.padding(0, 0, 5, 0));
        p.add(l);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(field);
        return p;
    }

    private void wireDobSync() {
        dobBsField.getDocument().addDocumentListener(new SimpleDocListener(this::onBsTyped));
        dobAdField.getDocument().addDocumentListener(new SimpleDocListener(this::onAdTyped));
    }

    private void onBsTyped() {
        if (syncing) return;
        String txt = dobBsField.getText().trim();
        if (txt.isEmpty()) { setHint(" ", UI.TEXT_SECONDARY); syncingSet(dobAdField, ""); return; }
        BSDate bs = BSDateConverter.parse(txt);
        if (bs == null) { setHint("Invalid BS date (yyyy-mm-dd)", UI.DANGER); return; }
        try {
            LocalDate ad = BSDateConverter.toAD(bs.year(), bs.month(), bs.day());
            syncingSet(dobAdField, ad.toString());
            setHint("= " + bs.display() + "  \u2194  " + DateUtil.display(ad), UI.SUCCESS);
        } catch (Exception ex) { setHint(ex.getMessage(), UI.DANGER); }
    }

    private void onAdTyped() {
        if (syncing) return;
        String txt = dobAdField.getText().trim();
        if (txt.isEmpty()) { setHint(" ", UI.TEXT_SECONDARY); syncingSet(dobBsField, ""); return; }
        LocalDate ad = DateUtil.parse(txt);
        if (ad == null) { setHint("Invalid AD date (yyyy-mm-dd)", UI.DANGER); return; }
        try {
            BSDate bs = BSDateConverter.toBS(ad);
            syncingSet(dobBsField, bs.format());
            setHint("= " + bs.display() + "  \u2194  " + DateUtil.display(ad), UI.SUCCESS);
        } catch (Exception ex) { setHint(ex.getMessage(), UI.DANGER); }
    }

    private void syncingSet(JTextField f, String text) {
        syncing = true;
        try { f.setText(text); } finally { syncing = false; }
    }
    private void setHint(String text, Color color) {
        dobHint.setText(text);
        dobHint.setForeground(color);
    }

    private void onSave() {
        String name    = nameField.getText().trim();
        String parent  = parentNameField.getText().trim();
        String phone   = parentPhoneField.getText().trim();
        String address = addressArea.getText().trim();
        Child.Gender sex = (Child.Gender) genderBox.getSelectedItem();
        LocalDate dob = DateUtil.parse(dobAdField.getText());

        if (name.isEmpty())     { warn("Child's name is required"); return; }
        if (dob == null)        { warn("Please enter a valid DOB in either BS or AD"); return; }
        if (dob.isAfter(LocalDate.now())) { warn("DOB cannot be in the future"); return; }
        if (dob.isBefore(LocalDate.now().minusYears(18))) { warn("DOB seems too old (>18 years)"); return; }
        if (parent.isEmpty())   { warn("Parent's name is required"); return; }
        if (!phone.matches("\\+?\\d{7,15}")) { warn("Parent's phone looks invalid"); return; }

        User me = AuthService.getCurrentUser();
        Child c = new Child();
        c.setName(name); c.setDateOfBirth(dob); c.setGender(sex);
        c.setParentName(parent); c.setParentPhone(phone); c.setAddress(address);
        c.setRegisteredBy(me == null ? 0 : me.getId());

        ChildRegistrationService.Result result;
        try { result = service.register(c); }
        catch (RuntimeException ex) {
            ex.printStackTrace();
            warn("Could not save: " + ex.getMessage());
            return;
        }

        CardActionDialog.show(this, result.child(), result.records(),
                "Registration successful",
                "<b>" + escapeHtml(result.child().getName()) + "</b> registered successfully.");

        clearForm();
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg,
                "Please check the form", JOptionPane.WARNING_MESSAGE);
    }

    private void clearForm() {
        syncingSet(dobBsField, ""); syncingSet(dobAdField, "");
        nameField.setText(""); genderBox.setSelectedIndex(0);
        parentNameField.setText(""); parentPhoneField.setText("");
        addressArea.setText("");
        setHint(" ", UI.TEXT_SECONDARY);
        nameField.requestFocus();
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static class SimpleDocListener implements DocumentListener {
        private final Runnable r;
        SimpleDocListener(Runnable r) { this.r = r; }
        @Override public void insertUpdate (DocumentEvent e) { r.run(); }
        @Override public void removeUpdate (DocumentEvent e) { r.run(); }
        @Override public void changedUpdate(DocumentEvent e) { r.run(); }
    }
}