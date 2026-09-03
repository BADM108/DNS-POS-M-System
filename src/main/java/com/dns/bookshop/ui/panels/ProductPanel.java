package com.dns.bookshop.ui.panels;

import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.services.ProductService;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.util.BarcodeLabelPrinter;
import com.dns.bookshop.util.ScannerInput;

import javax.swing.BorderFactory;
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Product registry: register items either by scanning an existing barcode,
 * by auto-generating a barcode (book ISBN or stationery), and print labels.
 */
public class ProductPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final ProductService productService = new ProductService();

    private JTextField searchField, nameField, barcodeField, supplierField,
            purchaseField, sellField, stockField, thresholdField;
    private JComboBox<String> categoryCombo;
    private JCheckBox barcodeScanMode, autoGenerateBook, autoGenerateItem;
    private JTable table;
    private DefaultTableModel model;
    private Product editing; // null => add mode

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    public ProductPanel() {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        build();
        refreshTable();
    }

    private void build() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UI.title("Product Registry & Barcodes"), BorderLayout.WEST);

        searchField = new JTextField(18);
        searchField.putClientProperty("JTextField.placeholderText", "Search name / barcode...");
        searchField.addActionListener(e -> loadIntoTable(productService.search(searchField.getText())));
        titleRow.add(searchField, BorderLayout.EAST);
        top.add(titleRow, BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildForm(), buildTable());
        split.setResizeWeight(0.42);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildForm() {
        JPanel form = UI.card();
        form.setLayout(new BorderLayout());
        form.setPreferredSize(new Dimension(360, 0));

        JLabel hdr = UI.section(editing == null ? "Register New Product" : "Edit Product");
        JPanel hdrRow = new JPanel(new BorderLayout());
        hdrRow.setOpaque(false);
        hdrRow.add(hdr, BorderLayout.WEST);
        form.add(hdrRow, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Product name *"), gc);
        gc.gridx = 1;
        nameField = new JTextField(16);
        body.add(nameField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Category *"), gc);
        gc.gridx = 1;
        categoryCombo = new JComboBox<>(new String[]{"Book", "Stationery"});
        categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        body.add(categoryCombo, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Supplier"), gc);
        gc.gridx = 1;
        supplierField = new JTextField(16);
        body.add(supplierField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Purchase price"), gc);
        gc.gridx = 1;
        purchaseField = new JTextField(16);
        body.add(purchaseField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Selling price *"), gc);
        gc.gridx = 1;
        sellField = new JTextField(16);
        body.add(sellField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Stock qty"), gc);
        gc.gridx = 1;
        stockField = new JTextField("0", 16);
        body.add(stockField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Low stock alert at"), gc);
        gc.gridx = 1;
        thresholdField = new JTextField("5", 16);
        body.add(thresholdField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        gc.gridwidth = 2;
        JLabel barcodeTitle = new JLabel("Barcode");
        barcodeTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        body.add(barcodeTitle, gc);
        gc.gridwidth = 1;

        row++;
        gc.gridx = 0; gc.gridy = row;
        body.add(UI.label("Barcode"), gc);
        gc.gridx = 1;
        barcodeField = new JTextField(16);
        JPanel barWrap = new JPanel(new BorderLayout(6, 0));
        barWrap.setOpaque(false);
        barWrap.add(barcodeField, BorderLayout.CENTER);
        body.add(barWrap, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        autoGenerateBook = new JCheckBox("Auto-generate ISBN barcode (for books)");
        autoGenerateBook.setSelected(true);
        body.add(autoGenerateBook, gc);
        gc.gridwidth = 1;

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        autoGenerateItem = new JCheckBox("Auto-generate new barcode (stationery)");
        body.add(autoGenerateItem, gc);
        gc.gridwidth = 1;

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        barcodeScanMode = new JCheckBox("Scan existing barcode below (e.g. supplier's)");
        body.add(barcodeScanMode, gc);
        gc.gridwidth = 1;

        row++;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        JLabel scanHint = new JLabel("<html><small>With scan mode ON, place cursor in the barcode "
                + "field and scan. It fills itself.</small></html>");
        scanHint.setFont(UIStyle.SMALL);
        scanHint.setForeground(UIStyle.MUTED);
        body.add(scanHint, gc);
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
            if (barcodeScanMode.isSelected()) {
                barcodeField.setText(code);
                JOptionPane.showMessageDialog(this,
                        "Barcode captured: " + code + "\nFill in the details and press Save.",
                        "Scanned", JOptionPane.INFORMATION_MESSAGE);
            } else {
                addByBarcodeQuick(code);
            }
        });

        return form;
    }

    private void addByBarcodeQuick(String code) {
        Product p = productService.findByBarcode(code);
        if (p != null) {
            loadRowIntoForm(p);
            JOptionPane.showMessageDialog(this, "This barcode is already registered to: " + p.getName(),
                    "Already exists", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        barcodeField.setText(code);
        barcodeScanMode.setSelected(true);
        nameField.requestFocusInWindow();
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
        JButton barcode = UI.ghost("Print Barcode Label");
        barcode.addActionListener(e -> printBarcode());
        JButton lowStock = UI.ghost("Show Low Stock");
        lowStock.addActionListener(e -> loadIntoTable(productService.findLowStock()));
        JButton all = UI.ghost("Show All");
        all.addActionListener(e -> refreshTable());
        leftBtns.add(manage);
        leftBtns.add(barcode);
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
                    Product p = productService.findAll().stream()
                            .filter(x -> x.getId() == id).findFirst().orElse(null);
                    if (p != null) loadRowIntoForm(p);
                }
            }
        });
        JScrollPane sp = UI.table(table);
        right.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel info = new JLabel("Double-click a row to edit it.");
        info.setFont(UIStyle.SMALL);
        info.setForeground(UIStyle.MUTED);
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
                p.isLowStock() ? "YES" : ""
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
        barcodeScanMode.setSelected(false);
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
        barcodeScanMode.setSelected(false);
        nameField.requestFocusInWindow();
    }

    private void saveProduct() {
        if (!auth.hasPermission(Permissions.ADD_PRODUCTS)) {
            JOptionPane.showMessageDialog(this, "You do not have permission to register products.",
                    "Permission denied", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for prices and stock.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean auto = autoGenerateBook.isSelected() || autoGenerateItem.isSelected();
        boolean asBook = autoGenerateBook.isSelected();

        try {
            if (editing != null) {
                // Preserve existing barcode unless a new one was entered.
                if (p.getBarcode() == null && editing.getBarcode() != null) {
                    p.setBarcode(editing.getBarcode());
                    p.setBarcodeGenerated(editing.isBarcodeGenerated());
                }
                productService.update(p);
                JOptionPane.showMessageDialog(this, "Product updated.", "Done",
                        JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not saved",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void manageStock() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!auth.hasPermission(Permissions.MANAGE_STOCK)) {
            JOptionPane.showMessageDialog(this, "No permission to manage stock.",
                    "Permission denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int id = (int) model.getValueAt(r, 0);
        Product p = productService.findAll().stream()
                .filter(x -> x.getId() == id).findFirst().orElse(null);
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
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        refreshTable();
    }

    private void printBarcode() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!auth.hasPermission(Permissions.GENERATE_BARCODES)) {
            JOptionPane.showMessageDialog(this, "No permission to print barcodes.",
                    "Permission denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int id = (int) model.getValueAt(r, 0);
        Product p = productService.findAll().stream()
                .filter(x -> x.getId() == id).findFirst().orElse(null);
        if (p == null) return;
        if (p.getBarcode() == null || p.getBarcode().isEmpty()) {
            JOptionPane.showMessageDialog(this, "This product has no barcode yet.",
                    "No barcode", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new BarcodeLabelPrinter(p).print();
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first.", "No selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!auth.hasPermission(Permissions.DELETE_PRODUCTS)) {
            JOptionPane.showMessageDialog(this, "No permission to delete products.",
                    "Permission denied", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
