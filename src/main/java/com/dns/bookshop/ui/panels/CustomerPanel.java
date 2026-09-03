package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.repositories.CustomerRepository;
import com.dns.bookshop.models.Customer;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Manage customers. Workers can view but (by default) adding is admin-only;
 * the admin can grant more via permissions.
 */
public class CustomerPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final CustomerRepository repo = new CustomerRepository();

    private JTextField searchField, nameField, phoneField, emailField, addrField;
    private JTable table;
    private DefaultTableModel model;
    private Customer editing;

    public CustomerPanel() {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        build();
        refresh();
    }

    private void build() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UI.title("Customers"), BorderLayout.WEST);
        searchField = new JTextField(18);
        searchField.putClientProperty("JTextField.placeholderText", "Search name / phone...");
        searchField.addActionListener(e -> refresh());
        top.add(searchField, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildForm(), buildTable());
        split.setResizeWeight(0.32);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildForm() {
        JPanel form = UI.card();
        form.setPreferredSize(new java.awt.Dimension(300, 0));
        form.setLayout(new BorderLayout());
        form.add(UI.section("Add Customer"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;
        gc.gridx = 0; gc.gridy = y;
        body.add(UI.label("Name *"), gc);
        gc.gridx = 1;
        nameField = new JTextField(14);
        body.add(nameField, gc);
        y++;
        gc.gridx = 0; gc.gridy = y;
        body.add(UI.label("Phone"), gc);
        gc.gridx = 1;
        phoneField = new JTextField(14);
        body.add(phoneField, gc);
        y++;
        gc.gridx = 0; gc.gridy = y;
        body.add(UI.label("Email"), gc);
        gc.gridx = 1;
        emailField = new JTextField(14);
        body.add(emailField, gc);
        y++;
        gc.gridx = 0; gc.gridy = y;
        body.add(UI.label("Address"), gc);
        gc.gridx = 1;
        addrField = new JTextField(14);
        body.add(addrField, gc);
        form.add(body, BorderLayout.CENTER);

        JButton save = UI.success("Save Customer");
        save.addActionListener(e -> save());
        JButton reset = UI.ghost("Reset");
        reset.addActionListener(e -> resetForm());
        JPanel btns = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        btns.setOpaque(false);
        btns.add(save);
        btns.add(reset);
        form.add(btns, BorderLayout.SOUTH);
        return form;
    }

    private JPanel buildTable() {
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        model = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Email", "Address"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    List<Customer> all = searchList();
                    int r = table.getSelectedRow();
                    if (r >= 0 && r < all.size()) loadIntoForm(all.get(r));
                }
            }
        });
        JScrollPane sp = UI.table(table);
        right.add(sp, BorderLayout.CENTER);
        JButton del = UI.danger("Delete Selected");
        del.addActionListener(e -> deleteSelected());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(new JLabel("Double-click to edit."), BorderLayout.WEST);
        bottom.add(del, BorderLayout.EAST);
        right.add(bottom, BorderLayout.SOUTH);
        return right;
    }

    private List<Customer> searchList() {
        String q = searchField.getText();
        return q.isEmpty() ? repo.findAll() : repo.search(q);
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (Customer c : searchList()) {
            model.addRow(new Object[]{c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress()});
        }
    }

    private void loadIntoForm(Customer c) {
        editing = c;
        nameField.setText(c.getName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        addrField.setText(c.getAddress());
    }

    private void resetForm() {
        editing = null;
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addrField.setText("");
    }

    private void save() {
        if (!auth.hasPermission(Permissions.ADD_CUSTOMERS)) {
            JOptionPane.showMessageDialog(this,
                    "You do not have permission to add or edit customers. Ask the admin.",
                    "Permission denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Customer c = editing != null ? editing : new Customer(name, phoneField.getText().trim(),
                emailField.getText().trim(), addrField.getText().trim());
        if (editing == null) {
            repo.create(c);
        } else {
            c.setName(name);
            c.setPhone(phoneField.getText().trim());
            c.setEmail(emailField.getText().trim());
            c.setAddress(addrField.getText().trim());
            repo.update(c);
        }
        resetForm();
        refresh();
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        if (!auth.hasPermission(Permissions.DELETE_CUSTOMERS)) {
            JOptionPane.showMessageDialog(this, "No permission to delete customers.",
                    "Permission denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected customer?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) model.getValueAt(r, 0);
            repo.delete(id);
            refresh();
        }
    }
}
