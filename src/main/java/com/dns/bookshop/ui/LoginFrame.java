package com.dns.bookshop.ui;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.RoundedPanel;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Modern, centered sign-in screen with a decorative brand panel on the left
 * and a clean form card on the right.
 */
public class LoginFrame extends JFrame {

    private final AuthService auth = AuthService.getInstance();
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginBtn;

    public LoginFrame() {
        super("DNS BookShop - Sign In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 560);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(760, 480));
        setContentPane(build());
    }

    private JPanel build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIStyle.BG);

        root.add(buildBrandPane(), BorderLayout.WEST);
        root.add(buildFormPane(), BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginBtn);
        return root;
    }

    /** Deep indigo brand/hero panel on the left. */
    private Component buildBrandPane() {
        RoundedPanel brand = new RoundedPanel(new Color(0x1E1B4B), 0);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setPreferredSize(new Dimension(360, 0));
        brand.setBorder(new javax.swing.border.EmptyBorder(48, 40, 48, 40));

        JLabel logo = new JLabel("DNS");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 44));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(logo);

        brand.add(Box.createVerticalStrut(10));
        JLabel name = new JLabel("BOOKSHOP");
        name.setFont(new Font("Segoe UI", Font.BOLD, 20));
        name.setForeground(UIStyle.PRIMARY_LIGHT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(name);

        brand.add(Box.createVerticalStrut(28));

        JLabel tag = new JLabel("<html>Point of Sale &amp; Inventory<br>"
                + "for books &amp; stationery.</html>");
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tag.setForeground(new Color(0xC7D2FE));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(tag);

        brand.add(Box.createVerticalStrut(16));
        JLabel localBadge = new JLabel("● 100% local · no server needed");
        localBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        localBadge.setForeground(new Color(0xA5B4FC));
        localBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(localBadge);

        brand.add(Box.createVerticalStrut(16));
        JLabel ver = new JLabel("App v" + AppConfig.APP_VERSION);
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ver.setForeground(new Color(0x818CF8));
        ver.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(ver);

        brand.add(Box.createVerticalGlue());
        return brand;
    }

    /** Centered sign-in card on the right. */
    private Component buildFormPane() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(UIStyle.BG);

        JPanel card = UI.card();
        card.setPreferredSize(new Dimension(380, 360));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel hdr = new JLabel("Welcome back");
        hdr.setFont(UIStyle.TITLE);
        hdr.setForeground(UIStyle.TEXT);
        hdr.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(hdr);

        card.add(Box.createVerticalStrut(6));
        JLabel sub = new JLabel("Sign in to your shop account");
        sub.setFont(UIStyle.NORMAL);
        sub.setForeground(UIStyle.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sub);

        card.add(Box.createVerticalStrut(26));

        card.add(sectionLabel("Username"));
        usernameField = UI.textField();
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(usernameField);

        card.add(Box.createVerticalStrut(14));

        card.add(sectionLabel("Password"));
        passwordField = new JPasswordField();
        passwordField.setFont(UIStyle.NORMAL);
        passwordField.setForeground(UIStyle.TEXT);
        passwordField.setCaretColor(UIStyle.PRIMARY);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new UI.RoundedBorder(new Color(0xD1D5DB), 6),
                new javax.swing.border.EmptyBorder(7, 10, 7, 10)));
        card.add(passwordField);

        card.add(Box.createVerticalStrut(24));

        loginBtn = UI.primary("Sign In  \u2192");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(loginBtn);

        card.add(Box.createVerticalStrut(18));

        JButton hint = UI.textButton("First time? Default: admin / admin", UIStyle.TEXT_MUTED);
        hint.setFont(UIStyle.SMALL);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.addActionListener(e -> {
            usernameField.setText("admin");
            passwordField.setText("admin");
            usernameField.requestFocusInWindow();
        });
        card.add(hint);

        loginBtn.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        JPanel gb = new JPanel(new GridBagLayout());
        gb.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0;
        gb.add(card, c);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new javax.swing.border.EmptyBorder(24, 40, 24, 40));
        center.add(gb, BorderLayout.CENTER);
        wrap.add(center);
        return wrap;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIStyle.SMALL_BOLD);
        l.setForeground(UIStyle.TEXT_MUTED);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private void doLogin() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            Toast.showError(this, "Please enter username and password.");
            return;
        }
        try {
            auth.login(user, pass);
            MainFrame main = new MainFrame();
            main.setVisible(true);
            dispose();
        } catch (IllegalArgumentException ex) {
            Toast.showError(this, ex.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
}
