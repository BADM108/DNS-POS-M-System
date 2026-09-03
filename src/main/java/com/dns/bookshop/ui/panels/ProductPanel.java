package com.dns.bookshop.ui.panels;

import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.services.ProductService;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.util.BarcodeLabelPrinter;
import com.dns.bookshop.util.ScannerInput;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.io.File;
import java.util.List;

/**
 * Product registry: register items either by scanning an existing barcode,
 * by auto-generating a barcode (book ISBN or stationery), and print labels.
 */
public class ProductPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final ProductService productService = new ProductService();
    private final java.awt.Window owner;

    private JTextField searchField, nameField, barcodeField, supplierField,
            purchaseField, sellField, stockField, thresholdField;
    private JComboBox<String> categoryCombo;
    private JCheckBox autoGenerateBook, autoGenerateItem;
    private JTable table;
    private DefaultTableModel model;
    private Product editing; // null => add mode

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    public ProductPanel(java.awt.Window owner) {
        super(new BorderLayout());
        this.owner = owner;
        setBackground(UIStyle.BG);
        setBorder(new javax.swing.border.EmptyBorder(16, 20, 16, 20));
        build();
        refreshTable();
    }

    private void build() {
        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);

        JButton addNew = UI.primary("+ New Product");
        addNew.addActionListener(e -> resetForm());
        top.add(addNew, BorderLayout.WEST);

        searchField = UI.textField();
        searchField.putClientProperty("JTextField.placeholderText", "Search name / barcode...");
        searchField.setPreferredSize(new Dimension(260, 38));
        searchField.addActionListener(e -> loadIntoTable(productService.search(searchField.getText())));
        top.add(searchField, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildForm(), buildTable());
        split.setResizeWeight(0.38);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildForm() {
        JPanel form = UI.card();
        form.setLayout(new BorderLayout(0, 14));
        form.setPreferredSize(new Dimension(380, 0));

        JLabel hdr = UI.section(editing == null ? "Register New Product" : "Edit Product");
        form.add(hdr, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 6, 5, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addField(body, gc, row, "Product name *", nameField = UI.textField(14));
        row = addField(body, gc, row, "Category *",
                categoryCombo = new JComboBox<>(new String[]{"Book", "Stationery"}));
        categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row = addField(body, gc, row, "Supplier", supplierField = UI.textField(14));
        row = addField(body, gc, row, "Purchase price", purchaseField = UI.textField(14));
        row = addField(body, gc, row, "Selling price *", sellField = UI.textField(14));
        stockField = UI.textField("0");
        row = addField(body, gc, row, "Stock qty", stockField);
        thresholdField = UI.textField("5");
        row = addField(body, gc, row, "Low stock alert at", thresholdField);

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        JLabel barcodeTitle = new JLabel("Barcode");
        barcodeTitle.setFont(UIStyle.NORMAL_BOLD);
        barcodeTitle.setForeground(UIStyle.TEXT);
        body.add(barcodeTitle, gc);
        gc.gridwidth = 1;

        row = addField(body, gc, row, "Barcode", barcodeField = UI.textField(14));

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        autoGenerateBook = new JCheckBox("Auto-generate ISBN barcode (books)");
        autoGenerateBook.setSelected(true);
        autoGenerateBook.setOpaque(false);
        autoGenerateBook.setFont(UIStyle.NORMAL);
        body.add(autoGenerateBook, gc);
        gc.gridwidth = 1;

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        autoGenerateItem = new JCheckBox("Auto-generate new barcode (stationery)");
        autoGenerateItem.setOpaque(false);
        autoGenerateItem.setFont(UIStyle.NORMAL);
        body.add(autoGenerateItem, gc);
        gc.gridwidth = 1;

        form.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        JButton save = UI.success(editing == null ? "Save Product" : "Save Changes");
        save.addActionListener(e -> saveProduct());
        JButton cancel = UI.ghost("Reset");
        cancel.addActionListener(e -> resetForm());
        actions.add(save);
        actions.add(cancel);
        form.add(actions, BorderLayout.SOUTH);

        ScannerInput.attach(barcodeField, code -> {
            // A scan always triggers the matching product directly:
            // - existing barcode  -> load that product into the form to view/update
            // - unknown barcode   -> start registering a brand new product with it
            addByBarcodeQuick(code);
        });

        return form;
    }

    private int addField(JPanel body, GridBagConstraints gc, int row, String label, java.awt.Component field) {
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label(label), gc);
        gc.gridx = 1;
        field.setPreferredSize(new Dimension(180, 34));
        body.add(field, gc);
        return row + 1;
    }

    private void addByBarcodeQuick(String code) {
        Product p = productService.findByBarcode(code);
        if (p != null) {
            loadRowIntoForm(p);
            Toast.showInfo(owner,
                    "Product found: " + p.getName() + " - edit details, then Save Changes.");
            return;
        }
        barcodeField.setText(code);
        nameField.requestFocusInWindow();
        Toast.showInfo(owner,
                "No product with barcode " + code + " yet - fill in the details and Save to register it.");
    }

    private JPanel buildTable() {
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);

        JPanel toolRow = new JPanel(new BorderLayout());
        toolRow.setOpaque(false);
        JPanel leftBtns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        leftBtns.setOpaque(false);
        JButton manage = UI.ghost("Manage Stock");
        manage.addActionListener(e -> manageStock());
        JButton barcode = UI.ghost("Print Label");
        barcode.addActionListener(e -> printBarcode());
        JButton pdf = UI.primary("Export Labels (PDF)");
        pdf.addActionListener(e -> exportPdf());
        JButton lowStock = UI.ghost("Low Stock");
        lowStock.addActionListener(e -> loadIntoTable(productService.findLowStock()));
        JButton all = UI.ghost("Show All");
        all.addActionListener(e -> refreshTable());
        leftBtns.add(manage);
        leftBtns.add(barcode);
        leftBtns.add(pdf);
        leftBtns.add(lowStock);
        leftBtns.add(all);
        toolRow.add(leftBtns, BorderLayout.WEST);
        right.add(toolRow, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Name", "Category", "Barcode", "Buy", "Sell", "Stock", "Low?"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int id = (int) model.getValueAt(table.getSelectedRow(), 0);
                    Product p = productService.findById(id);
                    if (p != null) loadRowIntoForm(p);
                }
            }
        });
        UI.styleTableWithZebra(table);
        for (int c : new int[]{4, 5, 6}) UI.alignColumn(table, c, JLabel.RIGHT);
        right.add(UI.table(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel info = new JLabel("Tip: scan a barcode to edit that product, or double-click a row. New items without a barcode get one auto-generated.");
        info.setFont(UIStyle.SMALL);
        info.setForeground(UIStyle.TEXT_MUTED);
        bottom.add(info, BorderLayout.WEST);
        JPanel delWrap = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        delWrap.setOpaque(false);
        JButton del = UI.danger("Delete Selected");
        del.addActionListener(e -> deleteSelected());
        delWrap.add(del);
        bottom.add(delWrap, BorderLayout.EAST);
        right.add(bottom, BorderLayout.SOUTH);

        return right;
    }

    private void refreshTable() {
        loadIntoTable(productService.findAll());
    }

    private void loadIntoTable(List<Product> products) {
        model.setRowCount(0);
        for (Product p : products) {
            model.addRow(new Object[]{
                p.getId(),
                p.getName(),
                ProductService.formatCategory(p.getCategory()),
                p.getBarcode(),
                MONEY.format(p.getPurchasePrice()),
                MONEY.format(p.getSellingPrice()),
                p.getStockQuantity(),
                p.isLowStock() ? "\u26A0" : ""
            });
        }
    }

    private void loadRowIntoForm(Product p) {
        if (p == null) return;
        editing = p;
        nameField.setText(p.getName());
        categoryCombo.setSelectedItem(ProductService.formatCategory(p.getCategory()));
        supplierField.setText(p.getSupplier());
        purchaseField.setText(String.valueOf(p.getPurchasePrice()));
        sellField.setText(String.valueOf(p.getSellingPrice()));
        stockField.setText(String.valueOf(p.getStockQuantity()));
        thresholdField.setText(String.valueOf(p.getLowStockThreshold()));
        barcodeField.setText(p.getBarcode());
        autoGenerateBook.setSelected(false);
        autoGenerateItem.setSelected(false);
    }

    private void resetForm() {
        editing = null;
        nameField.setText("");
        categoryCombo.setSelectedIndex(0);
        supplierField.setText("");
        purchaseField.setText("");
        sellField.setText("");
        stockField.setText("0");
        thresholdField.setText("5");
        barcodeField.setText("");
        autoGenerateBook.setSelected(true);
        autoGenerateItem.setSelected(false);
        nameField.requestFocusInWindow();
    }

    private void saveProduct() {
        if (!auth.hasPermission(Permissions.ADD_PRODUCTS)) {
            Toast.showError(owner, "You do not have permission to register products.");
            return;
        }
        Product p = new Product();
        if (editing != null) p.setId(editing.getId());

        p.setName(nameField.getText().trim());
        String cat = (String) categoryCombo.getSelectedItem();
        p.setCategory("Book".equals(cat) ? "BOOK" : "STATIONERY");
        p.setSupplier(supplierField.getText().trim());
        p.setBarcode(barcodeField.getText().trim().isEmpty() ? null : barcodeField.getText().trim());

        try {
            p.setPurchasePrice(dp(purchaseField.getText()));
            p.setSellingPrice(dp(sellField.getText()));
            p.setStockQuantity((int) di(stockField.getText()));
            p.setLowStockThreshold((int) di(thresholdField.getText()));
        } catch (NumberFormatException ex) {
            Toast.showError(owner, "Please enter valid numbers for prices and stock.");
            return;
        }

        boolean auto = autoGenerateBook.isSelected() || autoGenerateItem.isSelected();
        boolean asBook = autoGenerateBook.isSelected();

        try {
            if (editing != null) {
                if (p.getBarcode() == null && editing.getBarcode() != null) {
                    p.setBarcode(editing.getBarcode());
                    p.setBarcodeGenerated(editing.isBarcodeGenerated());
                }
                productService.update(p);
                Toast.showSuccess(owner, "Product updated.");
            } else {
                boolean askPrint = false;
                Product saved = productService.register(p, auto, asBook);
                askPrint = auto || p.isBarcodeGenerated();
                if (askPrint) {
                    int r = JOptionPane.showConfirmDialog(this,
                            "Product saved.\nBarcode: " + saved.getBarcode() +
                            "\n\nPrint a barcode label now?", "Print label",
                            JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (r == JOptionPane.YES_OPTION) {
                        new BarcodeLabelPrinter(saved).print();
                    }
                }
            }
            resetForm();
            refreshTable();
        } catch (IllegalArgumentException ex) {
            Toast.showError(owner, ex.getMessage());
        }
    }

    private void manageStock() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Toast.showInfo(owner, "Select a product first.");
            return;
        }
        if (!auth.hasPermission(Permissions.MANAGE_STOCK)) {
            Toast.showError(owner, "No permission to manage stock.");
            return;
        }
        int id = (int) model.getValueAt(r, 0);
        Product p = productService.findById(id);
        if (p == null) return;

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Product: " + p.getName() + "  (current stock: " + p.getStockQuantity() + ")"), gc);
        gc.gridy = 1;
        panel.add(new JLabel("Add stock (restock):"), gc);
        JSpinner addSpin = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
        gc.gridx = 1;
        panel.add(addSpin, gc);
        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Set absolute stock count:"), gc);
        JSpinner setSpin = new JSpinner(new SpinnerNumberModel(p.getStockQuantity(), 0, 100000, 1));
        gc.gridx = 1;
        panel.add(setSpin, gc);

        int choice = JOptionPane.showConfirmDialog(this, panel, "Manage Stock",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        int add = (Integer) addSpin.getValue();
        int set = (Integer) setSpin.getValue();
        try {
            if (add > 0) productService.restock(p.getId(), add, auth.getCurrentUser());
            if (add == 0) productService.setStock(p.getId(), set, auth.getCurrentUser());
        } catch (RuntimeException ex) {
            Toast.showError(owner, ex.getMessage());
        }
        refreshTable();
    }

    private void printBarcode() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Toast.showInfo(owner, "Select a product first.");
            return;
        }
        if (!auth.hasPermission(Permissions.GENERATE_BARCODES)) {
            Toast.showError(owner, "No permission to print barcodes.");
            return;
        }
        int id = (int) model.getValueAt(r, 0);
        Product p = productService.findById(id);
        if (p == null) return;
        if (p.getBarcode() == null || p.getBarcode().isEmpty()) {
            Toast.showWarning(owner, "This product has no barcode yet.");
            return;
        }
        new BarcodeLabelPrinter(p).print();
    }

    /**
     * Exports the selected product's barcode label (or the currently shown
     * list, if none is selected) to an A4 PDF sheet and opens it for printing.
     */
    private void exportPdf() {
        if (!auth.hasPermission(Permissions.GENERATE_BARCODES)) {
            Toast.showError(owner, "No permission to export barcodes.");
            return;
        }
        List<Product> products = new java.util.ArrayList<>();
        int sel = table.getSelectedRow();
        if (sel >= 0) {
            int id = (int) model.getValueAt(sel, 0);
            Product p = productService.findById(id);
            if (p != null) products.add(p);
        } else {
            for (int i = 0; i < model.getRowCount(); i++) {
                int id = (int) model.getValueAt(i, 0);
                Product p = productService.findById(id);
                if (p != null && p.getBarcode() != null && !p.getBarcode().isEmpty()) products.add(p);
            }
        }
        // Drop products that have no generated/existing barcode.
        products.removeIf(p -> p.getBarcode() == null || p.getBarcode().isEmpty());
        if (products.isEmpty()) {
            Toast.showInfo(owner, "No products with barcodes to export. Select a product, or add/generate barcodes first.");
            return;
        }
        try {
            File file = new com.dns.bookshop.util.BarcodePdfExporter().exportSheet(products);
            Toast.showSuccess(owner, "Label PDF exported to: " + file.getAbsolutePath());
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ignored) {
                // PDF written; user can open it manually if Desktop.open fails.
            }
        } catch (RuntimeException ex) {
            Toast.showError(owner, ex.getMessage());
        }
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Toast.showInfo(owner, "Select a product first.");
            return;
        }
        if (!auth.hasPermission(Permissions.DELETE_PRODUCTS)) {
            Toast.showError(owner, "No permission to delete products.");
            return;
        }
        int id = (int) model.getValueAt(r, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this product? This cannot be undone.", "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                productService.delete(id);
                refreshTable();
            } catch (RuntimeException ex) {
                Toast.showError(owner, ex.getMessage());
            }
        }
    }

    private double dp(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return Double.parseDouble(s.trim());
    }

    private double di(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return Double.parseDouble(s.trim());
    }

    @Override
    public void refresh() {
        refreshTable();
    }
}
