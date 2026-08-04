package com.hospital.vaccination.ui;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.model.User;
import com.hospital.vaccination.service.AuthService;
import com.hospital.vaccination.util.Icons;
import com.hospital.vaccination.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class AdminDashboard extends JFrame {

    private Sidebar sidebar;
    private NurseManagementPanel  nursePanel;
    private AllChildrenPanel      childrenPanel;
    private DefaultersReportPanel defaultersPanel;

    public AdminDashboard() {
        super(AppConfig.windowTitle("Admin Dashboard"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1250, 760);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        AppConfig.applyIcon(this);
        buildUI();
        installShortcuts();
    }

    private void buildUI() {
        User me = AuthService.getCurrentUser();

        nursePanel      = new NurseManagementPanel();
        childrenPanel   = new AllChildrenPanel();
        defaultersPanel = new DefaultersReportPanel();

        sidebar = new Sidebar(me, this::doLogout);
        sidebar.addNav("Nurses",             Icons.USERS,          nursePanel);
        sidebar.addNav("All Children",       Icons.CHILD,          childrenPanel);
        sidebar.addNav("Defaulters Report",  Icons.TRIANGLE_WARN,  defaultersPanel);

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
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_1, menu), "tab-1",
                () -> sidebar.selectByLabel("Nurses"));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_2, menu), "tab-2",
                () -> sidebar.selectByLabel("All Children"));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_3, menu), "tab-3",
                () -> sidebar.selectByLabel("Defaulters Report"));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), "search-focus", () -> {
            sidebar.selectByLabel("All Children");
            childrenPanel.focusSearch();
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh", () -> {
            nursePanel.refreshExternally();
            childrenPanel.refreshExternally();
            defaultersPanel.refreshExternally();
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