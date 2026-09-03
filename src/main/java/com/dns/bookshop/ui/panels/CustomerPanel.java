package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.repositories.CustomerRepository;
import com.dns.bookshop.models.Customer;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Manage customers. Workers can view but (by default) adding is admin-only;
 * the admin can grant more via permissions.
 */
public class CustomerPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final CustomerRepository repo = new CustomerRepository();
    private final java.awt.Window owner;

    private JTextField searchField, nameField, phoneField, emailField, addrField;
    private JTable table;
    private DefaultTableModel model;
    private Customer editing;

    public CustomerPanel(java.awt.Window owner) {
        super(new BorderLayout());
        this.owner = owner;
        setBackground(UIStyle.BG);
        setBorder(new javax.swing.border.EmptyBorder(16, 20, 16, 20));
        build();
        refresh();
    }

    private void build() {
        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        searchField = UI.textField();
        searchField.putClientProperty("JTextField.placeholderText", "Search name / phone...");
        searchField.setPreferredSize(new Dimension(260, 38));
        searchField.addActionListener(e -> refresh());
        top.add(searchField, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildForm(), buildTable());
        split.setResizeWeight(0.3);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildForm() {
        JPanel form = UI.card();
        form.setPreferredSize(new Dimension(320, 0));
        form.setLayout(new BorderLayout(0, 14));
        form.add(UI.section(editing == null ? "Add Customer" : "Edit Customer"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 6, 5, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;
        y = addField(body, gc, y, "Name *", nameField = UI.textField(14));
        y = addField(body, gc, y, "Phone", phoneField = UI.textField(14));
        y = addField(body, gc, y, "Email", emailField = UI.textField(14));
        y = addField(body, gc, y, "Address", addrField = UI.textField(14));
        form.add(body, BorderLayout.CENTER);

        JButton save = UI.success(editing == null ? "Save Customer" : "Save Changes");
        save.addActionListener(e -> save());
        JButton reset = UI.ghost("Reset");
        reset.addActionListener(e -> resetForm());
        JPanel btns = new JPanel(new GridLayout(1, 2, 8, 0));
        btns.setOpaque(false);
        btns.add(save);
        btns.add(reset);
        form.add(btns, BorderLayout.SOUTH);
        return form;
    }

    private int addField(JPanel body, GridBagConstraints gc, int y, String label, java.awt.Component field) {
        gc.gridx = 0; gc.gridy = y;
        body.add(UI.label(label), gc);
        gc.gridx = 1;
        field.setPreferredSize(new Dimension(180, 34));
        body.add(field, gc);
        return y + 1;
    }

    private JPanel buildTable() {
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        model = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Email", "Address"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    List<Customer> all = searchList();
                    int r = table.getSelectedRow();
                    if (r >= 0 && r < all.size()) loadIntoForm(all.get(r));
                }
            }
        });
        UI.styleTableWithZebra(table);
        right.add(UI.table(table), BorderLayout.CENTER);
        JButton del = UI.danger("Delete Selected");
        del.addActionListener(e -> deleteSelected());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(UI.muted("Double-click to edit."), BorderLayout.WEST);
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
            Toast.showError(owner, "You do not have permission to add or edit customers. Ask the admin.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Toast.showError(owner, "Name is required.");
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
            Toast.showError(owner, "No permission to delete customers.");
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
