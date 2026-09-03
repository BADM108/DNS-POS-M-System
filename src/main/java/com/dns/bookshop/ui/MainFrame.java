package com.dns.bookshop.ui;

import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.Icons;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.ui.panels.ChangePasswordDialog;
import com.dns.bookshop.ui.panels.CustomerPanel;
import com.dns.bookshop.ui.panels.DashboardPanel;
import com.dns.bookshop.ui.panels.PosPanel;
import com.dns.bookshop.ui.panels.ProductPanel;
import com.dns.bookshop.ui.panels.Refreshable;
import com.dns.bookshop.ui.panels.ReportPanel;
import com.dns.bookshop.ui.panels.SalesPanel;
import com.dns.bookshop.ui.panels.SettingsPanel;
import com.dns.bookshop.ui.panels.UsersPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main application shell: a dark grouped navigation sidebar + a light content
 * area. Panels are shown/hidden according to the logged-in user's permissions.
 */
public class MainFrame extends JFrame {

    private final AuthService auth = AuthService.getInstance();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JPanel> panelsByName = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private JPanel navPanel;
    private JLabel pageTitle;
    private String currentKey = "dashboard";

    // Sidebar geometry constants (px)
    private static final int SIDEBAR_W = 232;
    private static final int SIDEBAR_PAD = 16;

    public MainFrame() {
        super("DNS BookShop - Point of Sale");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        buildPanels();
        buildContent();
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMainArea(), BorderLayout.CENTER);
        setContentPane(root);

        try {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } catch (Exception ignored) {
            // Non-display environment; window still works at default size.
        }
        installShortcuts();
    }

    // ============================== Panels ==============================

    private void buildPanels() {
        panelsByName.put("dashboard", new DashboardPanel(this));
        panelsByName.put("pos", new PosPanel(this));
        panelsByName.put("products", new ProductPanel(this));
        panelsByName.put("customers", new CustomerPanel(this));
        panelsByName.put("users", new UsersPanel(this));
        panelsByName.put("sales", new SalesPanel(this));
        panelsByName.put("reports", new ReportPanel(this));
        panelsByName.put("settings", new SettingsPanel(this));

        for (JPanel p : panelsByName.values()) {
            content.add(p, p.getClass().getSimpleName());
        }
    }

    private void buildContent() {
        content.setBackground(UIStyle.BG);
        showPanel("dashboard");
    }

    // ============================== Sidebar ==============================

    private Component buildSidebar() {
        navPanel = new JPanel();
        navPanel.setBackground(UIStyle.SIDEBAR_BG);
        navPanel.setPreferredSize(new Dimension(SIDEBAR_W, 0));
        navPanel.setLayout(new StackFillLayout());
        navPanel.setBorder(new EmptyBorder(18, SIDEBAR_PAD, 18, SIDEBAR_PAD));

        navPanel.add(brandHeader());
        navPanel.add(Box.createVerticalStrut(22));

        // ---- Primary section ----
        addSectionHeader("Main");
        addNavItem("Dashboard", "dashboard", Icons.DASHBOARD, "dashboard", true);
        if (navVisible("pos")) addNavItem("Point of Sale", "pos", Icons.POS, Permissions.POS, false);
        navPanel.add(Box.createVerticalStrut(10));

        // ---- Management section ----
        addSectionHeader("Management");
        if (navVisible("products")) addNavItem("Products & Barcodes", "products", Icons.PRODUCTS, null, false);
        if (navVisible("customers")) addNavItem("Customers", "customers", Icons.CUSTOMERS, Permissions.VIEW_CUSTOMERS, false);
        if (navVisible("sales")) addNavItem("Sales History", "sales", Icons.SALES, Permissions.VIEW_SALES, false);
        if (navVisible("reports")) addNavItem("Reports & Analytics", "reports", Icons.REPORTS, Permissions.VIEW_REPORTS, false);
        navPanel.add(Box.createVerticalStrut(10));

        // ---- System section ----
        addSectionHeader("System");
        if (navVisible("users")) addNavItem("Workers & Permissions", "users", Icons.USERS, Permissions.MANAGE_USERS, false);
        if (auth.getCurrentUser().isAdmin()) {
            addNavItem("Settings", "settings", Icons.SETTINGS, null, true);
        }

        navPanel.add(Box.createVerticalGlue());

        navPanel.add(userFooter());

        return navPanel;
    }

    private Component brandHeader() {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(2, 2, 0, 2));

        JPanel identity = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        identity.setOpaque(false);
        identity.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Accent brand badge
        JLabel badge = new JLabel("DNS");
        badge.setOpaque(true);
        badge.setBackground(UIStyle.PRIMARY);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 14));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(38, 38));
        identity.add(badge);

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("BookShop");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandText.add(logo);
        JLabel tag = new JLabel("POS & Inventory");
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        tag.setForeground(UIStyle.SIDEBAR_TEXT_MUTED);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandText.add(tag);
        identity.add(brandText);

        brand.add(identity);
        return brand;
    }

    private void addSectionHeader(String label) {
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(UIStyle.SIDEBAR_TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(6, 10, 4, 0));
        navPanel.add(l);
        navPanel.add(Box.createVerticalStrut(4));
    }

    private boolean navVisible(String key) {
        switch (key) {
            case "pos": return auth.hasPermission(Permissions.POS);
            case "products": return true; // centrally filtered in addNavItem
            case "customers": return auth.hasPermission(Permissions.VIEW_CUSTOMERS);
            case "sales": return auth.hasPermission(Permissions.VIEW_SALES);
            case "reports": return auth.hasPermission(Permissions.VIEW_REPORTS);
            case "users": return auth.hasPermission(Permissions.MANAGE_USERS);
            default: return true;
        }
    }

    private void addNavItem(String text, String key, String glyph, String permission, boolean alwaysVisible) {
        boolean visible;
        if (alwaysVisible) {
            visible = true;
        } else if ("products".equals(key)) {
            visible = auth.hasPermission(Permissions.ADD_PRODUCTS)
                    || auth.hasPermission(Permissions.EDIT_PRODUCTS)
                    || auth.hasPermission(Permissions.GENERATE_BARCODES)
                    || auth.hasPermission(Permissions.MANAGE_STOCK);
        } else {
            visible = permission == null || auth.hasPermission(permission);
        }
        if (!visible) return;

        JButton b = navItemButton(text, glyph, key);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setActionCommand(key);
        b.addActionListener(e -> showPanel(key));
        navButtons.put(key, b);
        navPanel.add(b);
        navPanel.add(Box.createVerticalStrut(4));
    }

    /**
     * Full-width navigation button. The active item gets a filled accent pill
     * with a white glyph + a left highlight bar; hover darkens the pill.
     */
    private JButton navItemButton(String text, String glyph, String key) {
        final int fillW = SIDEBAR_W - 2 * SIDEBAR_PAD; // 200
        JButton b = new JButton(text) {
            @Override public Dimension getPreferredSize() { return new Dimension(fillW, 40); }
            @Override public Dimension getMinimumSize()   { return new Dimension(fillW, 40); }
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = currentKey.equals(key);
                boolean hover = !active && getModel().isRollover();
                Color fill = active ? UIStyle.SIDEBAR_ACTIVE
                        : hover ? UIStyle.SIDEBAR_BG_HOVER : UIStyle.SIDEBAR_BG;
                g2.setColor(fill);
                g2.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 4, 9, 9);
                // Left accent bar on the active item.
                if (active) {
                    g2.setColor(UIStyle.PRIMARY_LIGHT);
                    g2.fillRoundRect(2, 10, 4, getHeight() - 20, 2, 2);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        boolean initiallyActive = currentKey.equals(key);
        b.setIcon(Icons.glyph(glyph, initiallyActive ? Color.WHITE : new Color(0x9CA3AF)));
        b.setForeground(initiallyActive ? Color.WHITE : UIStyle.SIDEBAR_TEXT);
        b.setIconTextGap(12);
        b.setFont(initiallyActive ? UIStyle.NAV_ACTIVE : UIStyle.NAV);
        b.setHorizontalAlignment(JButton.LEFT);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private boolean isCurrent(String key) {
        return currentKey.equals(key);
    }

    private Component userFooter() {
        JPanel footer = new JPanel(new GridBagLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 10, 2, 10));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(2, 0, 2, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        JLabel name = new JLabel(auth.getCurrentUser().getFullName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 13));
        name.setForeground(Color.WHITE);
        footer.add(name, gc);

        gc.gridy = 1;
        JLabel role = new JLabel(auth.getCurrentUser().isAdmin() ? "Administrator" : "Shop Worker");
        role.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        role.setForeground(UIStyle.SIDEBAR_TEXT_MUTED);
        footer.add(role, gc);

        gc.gridy = 2;
        gc.insets = new Insets(8, 0, 0, 0);
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton pass = smallFooterButton("Change password", Icons.KEY);
        pass.addActionListener(e -> new ChangePasswordDialog(this).showDialog());
        actions.add(pass);

        JButton logout = smallFooterButton("Log out", Icons.LOGOUT);
        logout.setForeground(new Color(0xFCA5A5));
        logout.addActionListener(e -> logout());
        actions.add(logout);

        footer.add(actions, gc);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x1F2937)));
        wrap.setBackground(UIStyle.SIDEBAR_BG);
        wrap.add(footer, BorderLayout.CENTER);
        return wrap;
    }

    private JButton smallFooterButton(String text, String glyph) {
        JButton b = new JButton(text);
        b.setIcon(Icons.glyph(glyph, new Color(0x9CA3AF), 14));
        b.setIconTextGap(6);
        b.setFont(UIStyle.SMALL);
        b.setForeground(UIStyle.SIDEBAR_TEXT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setMargin(new Insets(2, 0, 2, 6));
        return b;
    }

    // ============================== Main area ==============================

    private Component buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UIStyle.BG);
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        return main;
    }

    private Component buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIStyle.HEADER_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyle.BORDER),
                new EmptyBorder(14, 24, 14, 24)));

        JPanel left = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        pageTitle = new JLabel("Dashboard");
        pageTitle.setFont(UIStyle.PAGE_TITLE);
        pageTitle.setForeground(UIStyle.TEXT);
        left.add(pageTitle);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel shop = new JLabel(com.dns.bookshop.db.Database.getInstance()
                .getSetting("shop.name", com.dns.bookshop.config.AppConfig.SHOP_NAME));
        shop.setFont(UIStyle.SMALL_BOLD);
        shop.setForeground(UIStyle.TEXT_MUTED);
        right.add(shop);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ============================== Navigation ==============================

    private void showPanel(String key) {
        currentKey = key;
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey().equals(key);
            e.getValue().setForeground(active ? Color.WHITE : UIStyle.SIDEBAR_TEXT);
            e.getValue().setFont(active ? UIStyle.NAV_ACTIVE : UIStyle.NAV);
            if (active) {
                e.getValue().setIcon(Icons.glyph(iconFor(key), Color.WHITE));
            } else {
                e.getValue().setIcon(Icons.glyph(iconFor(e.getKey()), new Color(0x9CA3AF)));
            }
            e.getValue().repaint();
        }

        String panelName = panelNameFor(key);
        if (panelName != null) {
            cards.show(content, panelName);
            JPanel p = panelsByName.get(key);
            if (p instanceof Refreshable) {
                ((Refreshable) p).refresh();
            }
            if (pageTitle != null) pageTitle.setText(titleFor(key));
        } else {
            cards.show(content, "DashboardPanel");
            if (pageTitle != null) pageTitle.setText("Dashboard");
        }
    }

    public void navigateTo(String key) {
        showPanel(key);
    }

    /**
     * Global keyboard navigation: Ctrl+1..8 jump to the panels quickly.
     * Always binds Ctrl+P for the Point of Sale so cashiers can get there fast.
     */
    private void installShortcuts() {
        final String[] order = {"dashboard", "pos", "products", "customers", "sales", "reports", "users", "settings"};
        for (int i = 0; i < order.length; i++) {
            final String key = order[i];
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, InputEvent.CTRL_DOWN_MASK),
                            "nav-" + key);
            getRootPane().getActionMap().put("nav-" + key, shortcut(key));
        }
        // Convenience: Ctrl+P always goes straight to the POS.
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "nav-pos");
        getRootPane().getActionMap().put("nav-pos", shortcut("pos"));
    }

    private javax.swing.Action shortcut(final String key) {
        return new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                // Only navigate to panels the user is allowed to see.
                if ("dashboard".equals(key) || navButtons.containsKey(key)) {
                    showPanel(key);
                }
            }
        };
    }

    private String iconFor(String key) {
        switch (key) {
            case "dashboard": return Icons.DASHBOARD;
            case "pos": return Icons.POS;
            case "products": return Icons.PRODUCTS;
            case "customers": return Icons.CUSTOMERS;
            case "sales": return Icons.SALES;
            case "reports": return Icons.REPORTS;
            case "users": return Icons.USERS;
            case "settings": return Icons.SETTINGS;
            default: return Icons.DASHBOARD;
        }
    }

    private String titleFor(String key) {
        switch (key) {
            case "dashboard": return "Dashboard";
            case "pos": return "Point of Sale";
            case "products": return "Products & Barcodes";
            case "customers": return "Customers";
            case "sales": return "Sales History";
            case "reports": return "Reports & Analytics";
            case "users": return "Workers & Permissions";
            case "settings": return "Settings";
            default: return "Dashboard";
        }
    }

    private String panelNameFor(String key) {
        switch (key) {
            case "dashboard": return "DashboardPanel";
            case "pos": return "PosPanel";
            case "products": return "ProductPanel";
            case "customers": return "CustomerPanel";
            case "users": return "UsersPanel";
            case "sales": return "SalesPanel";
            case "reports": return "ReportPanel";
            case "settings": return "SettingsPanel";
            default: return null;
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

    /**
     * A simple vertical stack layout that gives every child its preferred
     * height but stretches each child to the full width of the container
     * (minus insets). Used for the sidebar so nav items form a clean,
     * full-width pill rail. Extra vertical space is left at the bottom.
     */
    private static final class StackFillLayout implements LayoutManager {
        @Override public void addLayoutComponent(String name, Component comp) { }
        @Override public void removeLayoutComponent(Component comp) { }

        @Override public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int w = 0, h = in.top + in.bottom;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                Dimension p = c.getPreferredSize();
                w = Math.max(w, p.width);
                h += p.height;
            }
            return new Dimension(w + in.left + in.right, h);
        }

        @Override public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int y = in.top;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                int h = c.getPreferredSize().height;
                c.setBounds(in.left, y, width, h);
                y += h;
            }
        }
    }
}
