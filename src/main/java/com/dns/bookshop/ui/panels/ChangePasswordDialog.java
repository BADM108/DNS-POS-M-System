package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.repositories.UserRepository;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.util.PasswordUtil;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ChangePasswordDialog {

    private final JFrame parent;
    private final UserRepository userRepo = new UserRepository();

    public ChangePasswordDialog(JFrame parent) {
        this.parent = parent;
    }

    public void showDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);

        JPasswordField current = new JPasswordField(16);
        JPasswordField newPass = new JPasswordField(16);
        JPasswordField confirm = new JPasswordField(16);

        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Current password:"), gc);
        gc.gridx = 1;
        panel.add(current, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("New password:"), gc);
        gc.gridx = 1;
        panel.add(newPass, gc);

        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Confirm new:"), gc);
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
            JOptionPane.showMessageDialog(parent, "New password must be at least 4 characters.",
                    "Weak password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!np.equals(cf)) {
            JOptionPane.showMessageDialog(parent, "New passwords do not match.",
                    "Mismatch", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!PasswordUtil.verify(cur, u.getPasswordHash())) {
            JOptionPane.showMessageDialog(parent, "Current password is incorrect.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        userRepo.changePassword(u.getId(), PasswordUtil.hash(np));
        JOptionPane.showMessageDialog(parent, "Password changed successfully.", "Done",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
