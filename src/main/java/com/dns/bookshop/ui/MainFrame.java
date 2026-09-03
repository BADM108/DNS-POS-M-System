package com.dns.bookshop.ui;

import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.ui.panels.ChangePasswordDialog;
import com.dns.bookshop.ui.panels.CustomerPanel;
import com.dns.bookshop.ui.panels.DashboardPanel;
import com.dns.bookshop.ui.panels.PosPanel;
import com.dns.bookshop.ui.panels.ProductPanel;
import com.dns.bookshop.ui.panels.ReportPanel;
import com.dns.bookshop.ui.panels.SalesPanel;
import com.dns.bookshop.ui.panels.SettingsPanel;
import com.dns.bookshop.ui.panels.UsersPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main application window with a left navigation sidebar and content area.
 * Panels are shown/hidden according to the logged-in user's permissions.
 */
public class MainFrame extends JFrame {

    private final AuthService auth = AuthService.getInstance();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JPanel> panelsByName = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private JPanel navPanel;
    private JLabel roleLabel;

    public MainFrame() {
        super("DNS BookShop - Point of Sale");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        buildPanels();
        buildNav();
        buildContent();

        JPanel root = new JPanel(new BorderLayout());
        root.add(navPanel, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        try {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } catch (Exception ignored) {
            // Non-display environment; the window still works at default size.
        }
    }

    private void buildPanels() {
        panelsByName.put("dashboard", new DashboardPanel());
        panelsByName.put("pos", new PosPanel());
        panelsByName.put("products", new ProductPanel());
        panelsByName.put("customers", new CustomerPanel());
        panelsByName.put("users", new UsersPanel());
        panelsByName.put("sales", new SalesPanel());
        panelsByName.put("reports", new ReportPanel());
        panelsByName.put("settings", new SettingsPanel());

        for (JPanel p : panelsByName.values()) {
            content.add(p, p.getClass().getSimpleName());
        }
    }

    private void buildNav() {
        navPanel = new JPanel();
        navPanel.setBackground(UIStyle.HEADER_BG);
        navPanel.setPreferredSize(new Dimension(230, 0));
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(20, 12, 20, 12));

        JLabel brand = new JLabel("DNS BOOKSHOP");
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(CENTER_ALIGNMENT);
        navPanel.add(brand);

        JLabel divider = new JLabel("───────────");
        divider.setForeground(new Color(0.4f, 0.45f, 0.55f));
        divider.setAlignmentX(CENTER_ALIGNMENT);
        navPanel.add(divider);
        navPanel.add(Box.createVerticalStrut(18));

        addNavItem("Dashboard", "dashboard", "dashboard", true);
        addNavItem("Point of Sale", "pos", Permissions.POS, false);
        addNavItem("Products & Barcodes", "products", Permissions.ADD_PRODUCTS, false);
        addNavItem("Customers", "customers", Permissions.VIEW_CUSTOMERS, false);
        addNavItem("Sales History", "sales", Permissions.VIEW_SALES, false);
        addNavItem("Reports", "reports", Permissions.VIEW_REPORTS, false);
        addNavItem("Workers & Permissions", "users", Permissions.MANAGE_USERS, false);
        // Settings is only shown to admins.
        if (auth.getCurrentUser().isAdmin()) {
            addNavItem("Settings", "settings", null, true);
        }

        navPanel.add(Box.createVerticalGlue());

        roleLabel = new JLabel();
        roleLabel.setForeground(new Color(0.7f, 0.75f, 0.8f));
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        roleLabel.setAlignmentX(CENTER_ALIGNMENT);
        navPanel.add(roleLabel);

        JLabel userId = new JLabel(auth.getCurrentUser().getFullName());
        userId.setForeground(Color.WHITE);
        userId.setFont(UIStyle.NORMAL);
        userId.setAlignmentX(CENTER_ALIGNMENT);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(userId);

        JButton changePass = navTextButton("Change password");
        changePass.setAlignmentX(CENTER_ALIGNMENT);
        changePass.addActionListener(e -> new ChangePasswordDialog(this).showDialog());
        navPanel.add(changePass);

        JButton logout = navTextButton("Logout");
        logout.setAlignmentX(CENTER_ALIGNMENT);
        logout.setForeground(new Color(1.0f, 0.6f, 0.6f));
        logout.addActionListener(e -> logout());
        navPanel.add(logout);

        updateRoleLabel();
    }

    private void addNavItem(String text, String key, String permission, boolean alwaysVisible) {
        boolean visible = alwaysVisible || auth.hasPermission(permission);
        if (!visible) return;
        JButton b = navTextButton(text);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setActionCommand(key);
        b.addActionListener(e -> showPanel(key));
        navButtons.put(key, b);
        navPanel.add(b);
        navPanel.add(Box.createVerticalStrut(6));
    }

    private void buildContent() {
        content.setBackground(UIStyle.BG);
        showPanel("dashboard");
    }

    private void showPanel(String key) {
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            e.getValue().setForeground(e.getKey().equals(key) ? Color.WHITE : new Color(0.75f, 0.8f, 0.85f));
            e.getValue().setBackground(e.getKey().equals(key) ? new Color(0.35f, 0.45f, 0.65f)
                    : new Color(0f, 0f, 0f, 0));
        }
        // Find panel by key.
        String panelName = null;
        switch (key) {
            case "dashboard": panelName = "DashboardPanel"; break;
            case "pos": panelName = "PosPanel"; break;
            case "products": panelName = "ProductPanel"; break;
            case "customers": panelName = "CustomerPanel"; break;
            case "users": panelName = "UsersPanel"; break;
            case "sales": panelName = "SalesPanel"; break;
            case "reports": panelName = "ReportPanel"; break;
            case "settings": panelName = "SettingsPanel"; break;
            default: break;
        }
        if (panelName != null) {
            cards.show(content, panelName);
            JPanel p = panelsByName.get(key);
            if (p instanceof com.dns.bookshop.ui.panels.Refreshable) {
                ((com.dns.bookshop.ui.panels.Refreshable) p).refresh();
            }
        }
    }

    private JButton navTextButton(String text) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(new Color(0.8f, 0.85f, 0.9f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(JButton.CENTER);
        return b;
    }

    private void updateRoleLabel() {
        if (auth.getCurrentUser() != null) {
            boolean admin = auth.getCurrentUser().isAdmin();
            roleLabel.setText(admin ? "ROLE: ADMIN" : "ROLE: WORKER");
        }
    }

    private void logout() {
        int r = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?",
                "Logout", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            auth.logout();
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
            dispose();
        }
    }
}
