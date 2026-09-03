package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.repositories.UserRepository;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.util.PasswordUtil;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ChangePasswordDialog {

    private final java.awt.Window parent;
    private final UserRepository userRepo = new UserRepository();

    public ChangePasswordDialog(java.awt.Window parent) {
        this.parent = parent;
    }

    public void showDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIStyle.SURFACE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);

        JPasswordField current = themeField(new JPasswordField(16));
        JPasswordField newPass = themeField(new JPasswordField(16));
        JPasswordField confirm = themeField(new JPasswordField(16));

        gc.gridx = 0; gc.gridy = 0;
        panel.add(UI.label("Current password:"), gc);
        gc.gridx = 1;
        panel.add(current, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(UI.label("New password:"), gc);
        gc.gridx = 1;
        panel.add(newPass, gc);

        gc.gridx = 0; gc.gridy = 2;
        panel.add(UI.label("Confirm new:"), gc);
        gc.gridx = 1;
        panel.add(confirm, gc);

        int r = JOptionPane.showConfirmDialog(parent, panel, "Change Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        User u = AuthService.getInstance().getCurrentUser();
        String cur = new String(current.getPassword());
        String np = new String(newPass.getPassword());
        String cf = new String(confirm.getPassword());

        if (np.length() < 4) {
            Toast.showWarning(parent, "New password must be at least 4 characters.");
            return;
        }
        if (!np.equals(cf)) {
            Toast.showError(parent, "New passwords do not match.");
            return;
        }
        if (!PasswordUtil.verify(cur, u.getPasswordHash())) {
            Toast.showError(parent, "Current password is incorrect.");
            return;
        }
        userRepo.changePassword(u.getId(), PasswordUtil.hash(np));
        Toast.showSuccess(parent, "Password changed successfully.");
    }

    private JPasswordField themeField(JPasswordField f) {
        f.setFont(UIStyle.NORMAL);
        f.setForeground(UIStyle.TEXT);
        f.setCaretColor(UIStyle.PRIMARY);
        f.setColumns(16);
        return f;
    }
}
