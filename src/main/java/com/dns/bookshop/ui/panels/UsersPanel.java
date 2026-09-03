package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.repositories.AuditLogRepository;
import com.dns.bookshop.db.repositories.UserRepository;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.util.PasswordUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin-only panel for managing workers and their permissions.
 */
public class UsersPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final UserRepository repo = new UserRepository();

    private JTable table;
    private DefaultTableModel model;
    private User currentWorker;

    // Permission editor widgets
    private JLabel permTitle;
    private JPanel permBody;
    private final Map<String, JCheckBox> boxes = new LinkedHashMap<>();
    private JTextField newPass;

    public UsersPanel() {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        build();
    }

    public static Permissions defaultWorkerPerms() {
        Permissions p = new Permissions();
        for (String key : new String[]{Permissions.POS, Permissions.ADD_PRODUCTS,
                Permissions.EDIT_PRODUCTS, Permissions.VIEW_CUSTOMERS, Permissions.VIEW_SALES,
                Permissions.VIEW_REPORTS, Permissions.MANAGE_STOCK,
                Permissions.REGISTER_SCANNED, Permissions.GENERATE_BARCODES,
                Permissions.BACKUP_DATA, Permissions.REFUND_SALES}) {
            p.grant(key);
        }
        return p;
    }

    private void build() {
        add(UI.title("Workers & Permissions"), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), buildRight());
        split.setResizeWeight(0.42);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);
        refreshTable();
    }

    private Component buildLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);

        JPanel addCard = UI.card();
        addCard.setLayout(new BorderLayout(0, 8));
        addCard.add(UI.section("Add Worker"), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField usernameF = new JTextField(14);
        JTextField fullNameF = new JTextField(14);
        JTextField passF = new JTextField(14);
        passF.setText("dns123");

        gc.gridx = 0; gc.gridy = 0; form.add(UI.label("Username *"), gc);
        gc.gridx = 1; form.add(usernameF, gc);
        gc.gridy = 1; gc.gridx = 0; form.add(UI.label("Full name *"), gc);
        gc.gridx = 1; form.add(fullNameF, gc);
        gc.gridy = 2; gc.gridx = 0; form.add(UI.label("Password *"), gc);
        gc.gridx = 1; form.add(passF, gc);

        JButton addBtn = UI.success("Create Worker");
        addBtn.addActionListener(e -> {
            try {
                String un = usernameF.getText().trim();
                String fn = fullNameF.getText().trim();
                if (un.isEmpty() || fn.isEmpty() || passF.getText().trim().isEmpty()) {
                    throw new IllegalArgumentException("All fields are required.");
                }
                if (passF.getText().trim().length() < 4) {
                    throw new IllegalArgumentException("Password must be at least 4 characters.");
                }
                if (repo.findByUsername(un) != null) {
                    throw new IllegalArgumentException("Username already exists.");
                }
                User u = new User(un, PasswordUtil.hash(passF.getText().trim()), fn, "WORKER");
                repo.create(u);
                repo.savePermissions(u.getId(), defaultWorkerPerms());
                new AuditLogRepository().log(auth.getCurrentUser().getId(), auth.getCurrentUser().getUsername(),
                        "USER_CREATE", "Added worker " + un);
                usernameF.setText("");
                fullNameF.setText("");
                refreshTable();
                JOptionPane.showMessageDialog(this, "Worker created. Tune their permissions in the panel on the right.",
                        "Done", JOptionPane.INFORMATION_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Not saved", JOptionPane.WARNING_MESSAGE);
            }
        });
        gc.gridy = 3; gc.gridx = 0; gc.gridwidth = 2;
        form.add(addBtn, gc);
        addCard.add(form, BorderLayout.CENTER);
        left.add(addCard, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Username", "Full Name", "Role", "Active"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int id = (int) model.getValueAt(table.getSelectedRow(), 0);
                loadPermissionsFor(id);
            }
        });
        left.add(UI.table(table), BorderLayout.CENTER);
        return left;
    }

    private Component buildRight() {
        JPanel card = UI.card();
        card.setLayout(new BorderLayout(0, 10));
        permTitle = UI.section("Select a worker to manage permissions");
        card.add(permTitle, BorderLayout.NORTH);

        permBody = new JPanel(new GridLayout(0, 2, 8, 8));
        permBody.setOpaque(false);
        permBody.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        addPerm("pos", "Operate POS / Make sales");
        addPerm("add_products", "Register new products");
        addPerm("edit_products", "Edit product details");
        addPerm("delete_products", "Delete products");
        addPerm("register_scanned", "Register items by scanning");
        addPerm("generate_barcodes", "Generate & print barcodes");
        addPerm("view_customers", "View customers");
        addPerm("add_customers", "Add / edit customers");
        addPerm("delete_customers", "Delete customers");
        addPerm("view_sales", "View sales history");
        addPerm("refund_sales", "Refund sales");
        addPerm("view_reports", "View reports / dashboard");
        addPerm("manage_stock", "Manage stock levels");
        addPerm("backup_data", "Backup / export data");
        card.add(new JScrollPane(permBody), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        JButton save = UI.success("Save Permissions");
        save.addActionListener(e -> savePermissions());
        JButton defaults = UI.ghost("Set Defaults");
        defaults.addActionListener(e -> {
            if (currentWorker != null) applyPerms(defaultWorkerPerms());
        });
        actions.add(save);
        actions.add(defaults);
        card.add(actions, BorderLayout.SOUTH);

        JPanel manage = new JPanel(new GridLayout(1, 2, 8, 0));
        manage.setOpaque(false);
        JButton deactivate = UI.ghost("Toggle Active");
        deactivate.addActionListener(e -> toggleActive());
        JButton delete = UI.danger("Delete Worker");
        delete.addActionListener(e -> deleteWorker());
        manage.add(deactivate);
        manage.add(delete);

        newPass = new JTextField();
        newPass.putClientProperty("JTextField.placeholderText", "New password (blank = keep)");
        JButton setPass = UI.ghost("Set Password");
        setPass.addActionListener(e -> setPassword());
        JPanel passRow = new JPanel(new BorderLayout(6, 0));
        passRow.setOpaque(false);
        passRow.add(newPass, BorderLayout.CENTER);
        passRow.add(setPass, BorderLayout.EAST);

        JPanel manageWrap = new JPanel(new BorderLayout(0, 8));
        manageWrap.setOpaque(false);
        manageWrap.add(manage, BorderLayout.NORTH);
        manageWrap.add(passRow, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(manageWrap, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private void addPerm(String key, String label) {
        JCheckBox cb = new JCheckBox(label);
        cb.setOpaque(false);
        boxes.put(key, cb);
        permBody.add(cb);
    }

    private void loadPermissionsFor(int userId) {
        User u = repo.findById(userId);
        if (u == null) return;
        if (u.isAdmin()) {
            currentWorker = null;
            permTitle.setText("Admin account - has all permissions.");
            boxes.values().forEach(b -> b.setSelected(true));
            return;
        }
        currentWorker = u;
        permTitle.setText("Permissions for " + u.getFullName() + " (" + u.getUsername() + ")");
        applyPerms(repo.getPermissions(u.getId()));
    }

    private void applyPerms(Permissions perms) {
        for (Map.Entry<String, JCheckBox> e : boxes.entrySet()) {
            e.getValue().setSelected(perms.has(e.getKey()));
        }
    }

    private void savePermissions() {
        if (currentWorker == null) {
            JOptionPane.showMessageDialog(this, "Select a worker first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Permissions perms = new Permissions();
        boxes.forEach((k, cb) -> perms.set(k, cb.isSelected()));
        repo.savePermissions(currentWorker.getId(), perms);
        new AuditLogRepository().log(auth.getCurrentUser().getId(), auth.getCurrentUser().getUsername(),
                "PERMISSIONS", "Updated permissions for " + currentWorker.getUsername());
        JOptionPane.showMessageDialog(this, "Permissions saved for " + currentWorker.getFullName(),
                "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    private void toggleActive() {
        if (currentWorker == null) {
            JOptionPane.showMessageDialog(this, "Select a worker first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        User u = repo.findById(currentWorker.getId());
        u.setActive(!u.isActive());
        repo.update(u);
        new AuditLogRepository().log(auth.getCurrentUser().getId(), auth.getCurrentUser().getUsername(),
                "USER_TOGGLE", "Set " + u.getUsername() + " active=" + u.isActive());
        refreshTable();
    }

    private void deleteWorker() {
        if (currentWorker == null) {
            JOptionPane.showMessageDialog(this, "Select a worker first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentWorker.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Cannot delete an admin.",
                    "Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete worker '" + currentWorker.getFullName() + "'? This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            repo.delete(currentWorker.getId());
            new AuditLogRepository().log(auth.getCurrentUser().getId(), auth.getCurrentUser().getUsername(),
                    "USER_DELETE", "Deleted worker " + currentWorker.getUsername());
            currentWorker = null;
            refreshTable();
        }
    }

    private void setPassword() {
        if (currentWorker == null) {
            JOptionPane.showMessageDialog(this, "Select a worker first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String pw = newPass.getText().trim();
        if (pw.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters.",
                    "Weak", JOptionPane.WARNING_MESSAGE);
            return;
        }
        repo.changePassword(currentWorker.getId(), PasswordUtil.hash(pw));
        newPass.setText("");
        JOptionPane.showMessageDialog(this, "Password updated.", "Done",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (User u : repo.findAll()) {
            model.addRow(new Object[]{u.getId(), u.getUsername(), u.getFullName(),
                    u.isAdmin() ? "ADMIN" : "WORKER", u.isActive() ? "Yes" : "No"});
        }
    }

    @Override
    public void refresh() {
        refreshTable();
    }
}
