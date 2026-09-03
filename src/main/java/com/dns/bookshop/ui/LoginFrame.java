package com.dns.bookshop.ui;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginFrame extends JFrame {

    private final AuthService auth = AuthService.getInstance();
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        super("DNS BookShop - Sign In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 420);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(build());
    }

    private JPanel build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIStyle.PRIMARY);

        // Header panel
        JPanel header = new JPanel();
        header.setBackground(UIStyle.PRIMARY);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 0, 10, 0));

        JLabel title = new JLabel("DNS BOOKSHOP");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Books & Stationery  -  Point of Sale");
        sub.setFont(UIStyle.SMALL);
        sub.setForeground(new Color(0.7f, 0.75f, 0.85f));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);
        root.add(header, BorderLayout.NORTH);

        // Card
        JPanel card = UI.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(10, 12, 10, 12);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel signLabel = new JLabel("Sign in to continue");
        signLabel.setFont(UIStyle.SECTION);
        signLabel.setForeground(UIStyle.PRIMARY);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        card.add(signLabel, gc);
        gc.gridwidth = 1;

        gc.gridx = 0; gc.gridy = 1;
        card.add(UI.label("Username"), gc);
        usernameField = new JTextField(18);
        gc.gridx = 1;
        card.add(usernameField, gc);

        gc.gridx = 0; gc.gridy = 2;
        card.add(UI.label("Password"), gc);
        passwordField = new JPasswordField(18);
        gc.gridx = 1;
        card.add(passwordField, gc);

        JButton loginBtn = UI.primary("Sign In");
        loginBtn.setMaximumSize(new Dimension(200, 40));
        loginBtn.setAlignmentX(CENTER_ALIGNMENT);
        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2;
        card.add(loginBtn, gc);

        JButton hint = new JButton("?");
        hint.setToolTipText("Default admin login:  admin / admin");
        hint.setFont(UIStyle.SMALL);
        hint.setBorderPainted(false);
        hint.setContentAreaFilled(false);
        hint.setFocusPainted(false);
        hint.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Default administrator:\n  Username: admin\n  Password: admin\n\n"
                + "You should change this after first login.",
                "First time here?", JOptionPane.INFORMATION_MESSAGE));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        footer.add(hint);
        gc.gridy = 4;
        card.add(footer, gc);

        loginBtn.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 40, 24, 40));
        wrap.add(card, BorderLayout.CENTER);
        root.add(wrap, BorderLayout.CENTER);

        JLabel ver = new JLabel("App v" + AppConfig.APP_VERSION);
        ver.setFont(new Font("SansSerif", Font.PLAIN, 10));
        ver.setForeground(new Color(0.6f, 0.65f, 0.75f));
        ver.setHorizontalAlignment(JLabel.CENTER);
        root.add(ver, BorderLayout.SOUTH);
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

        return root;
    }

    private void doLogin() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.",
                    "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            auth.login(user, pass);
            MainFrame main = new MainFrame();
            main.setVisible(true);
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
}
