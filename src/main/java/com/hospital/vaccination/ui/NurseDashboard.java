package com.hospital.vaccination.ui;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.model.User;
import com.hospital.vaccination.service.AuthService;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Nurse dashboard: sidebar-driven layout.
 * <ul>
 *   <li>Register new child</li>
 *   <li>Today's checklist</li>
 *   <li>All children</li>
 * </ul>
 */
public class NurseDashboard extends JFrame {

    private Sidebar sidebar;
    private ChildRegistrationPanel registerPanel;
    private TodayChecklistPanel    checklistPanel;
    private AllChildrenPanel       childrenPanel;

    public NurseDashboard() {
        super(AppConfig.windowTitle("Nurse Dashboard"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        AppConfig.applyIcon(this);
        buildUI();
        installShortcuts();
    }

    private void buildUI() {
        User me = AuthService.getCurrentUser();

        registerPanel  = new ChildRegistrationPanel();
        checklistPanel = new TodayChecklistPanel();
        childrenPanel  = new AllChildrenPanel();

        sidebar = new Sidebar(me, this::doLogout);
        sidebar.addNav("Register Child",    Icons.USER_PLUS, registerPanel);
        sidebar.addNav("Today's Checklist", Icons.CLIPBOARD, checklistPanel);
        sidebar.addNav("All Children",      Icons.CHILD,     childrenPanel);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UI.BG_APP);
        root.add(sidebar, BorderLayout.WEST);
        root.add(sidebar.getCardHost(), BorderLayout.CENTER);
        setContentPane(root);

        setJMenuBar(buildMenu());
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> new AboutDialog(this).setVisible(true));
        help.add(about);
        bar.add(help);
        return bar;
    }

    private void installShortcuts() {
        JRootPane root = getRootPane();
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_L, menu), "logout", this::doLogout);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, menu), "tab-register",
                () -> sidebar.selectByLabel("Register Child"));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_T, menu), "tab-checklist",
                () -> sidebar.selectByLabel("Today's Checklist"));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), "search-children", () -> {
            sidebar.selectByLabel("All Children");
            childrenPanel.focusSearch();
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh", () -> {
            checklistPanel.refreshExternally();
            childrenPanel.refreshExternally();
        });
    }

    private void bind(JRootPane root, KeyStroke ks, String name, Runnable r) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { r.run(); }
        });
    }

    private void doLogout() {
        AuthService.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
