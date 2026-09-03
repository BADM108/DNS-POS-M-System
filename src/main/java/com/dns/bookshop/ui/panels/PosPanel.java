package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.Database;
import com.dns.bookshop.db.repositories.CustomerRepository;
import com.dns.bookshop.models.Customer;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.models.Sale;
import com.dns.bookshop.models.SaleItem;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.services.ProductService;
import com.dns.bookshop.services.SaleService;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.util.BillPrinter;
import com.dns.bookshop.util.ScannerInput;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The Point of Sale screen: scan/type items into a live cart, see the running
 * total, take payment, and (optionally) print an A4 bill.
 */
public class PosPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();
    private final CustomerRepository customerRepo = new CustomerRepository();
    private final Window owner;

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final List<CartRow> cart = new ArrayList<>();
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JTextField scanField;
    private JComboBox<Customer> customerCombo;
    private JTextField discountField;
    private JLabel subtotalLabel, totalLabel, countLabel;

    private static class CartRow {
        Product product;
        int qty;
    }

    public PosPanel(Window owner) {
        super(new BorderLayout());
        this.owner = owner;
        setBackground(UIStyle.BG);
        setBorder(new javax.swing.border.EmptyBorder(16, 20, 16, 20));
        build();
    }

    private void build() {
        JPanel left = UI.card();
        left.setLayout(new BorderLayout(0, 12));

        JPanel scanRow = new JPanel(new BorderLayout(10, 0));
        scanRow.setOpaque(false);
        scanField = UI.textField();
        scanField.putClientProperty("JTextField.placeholderText", "Scan or type a barcode, then press Enter");
        scanField.setFont(UIStyle.NORMAL.deriveFont(16f));
        JButton addBtn = UI.primary("Add \uFF0B");
        addBtn.addActionListener(e -> addByBarcode(scanField.getText()));
        scanRow.add(scanField, BorderLayout.CENTER);
        scanRow.add(addBtn, BorderLayout.EAST);
        left.add(scanRow, BorderLayout.NORTH);

        cartModel = new DefaultTableModel(new String[]{"Item", "Barcode", "Qty", "Price", "Amount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        UI.styleTableWithZebra(cartTable);
        UI.alignColumn(cartTable, 2, JLabel.CENTER);
        UI.alignColumn(cartTable, 3, JLabel.RIGHT);
        UI.alignColumn(cartTable, 4, JLabel.RIGHT);
        left.add(UI.table(cartTable), BorderLayout.CENTER);

        ScannerInput.attach(scanField, this::addByBarcode);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(340, 0));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new javax.swing.border.EmptyBorder(0, 16, 0, 0));

        JPanel payCard = UI.card();
        payCard.setLayout(new BoxLayout(payCard, BoxLayout.Y_AXIS));
        payCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 560));

        JLabel payTitle = UI.section("Payment");
        payCard.add(payTitle);
        payCard.add(Box.createVerticalStrut(14));

        payCard.add(fieldLabel("Customer"));
        customerCombo = new JComboBox<>();
        customerCombo.addItem(null);
        for (Customer c : customerRepo.findAll()) customerCombo.addItem(c);
        customerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        payCard.add(customerCombo);
        payCard.add(Box.createVerticalStrut(12));

        payCard.add(fieldLabel("Discount (LKR)"));
        discountField = UI.textField("0");
        discountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        payCard.add(discountField);
        payCard.add(Box.createVerticalStrut(14));

        countLabel = new JLabel("0 items");
        countLabel.setFont(UIStyle.SMALL);
        countLabel.setForeground(UIStyle.TEXT_MUTED);
        payCard.add(countLabel);
        payCard.add(Box.createVerticalStrut(6));

        payCard.add(totalsRow("Subtotal", subtotalLabel = new JLabel("LKR 0.00"), false));
        payCard.add(Box.createVerticalStrut(6));
        payCard.add(totalsRow("TOTAL", totalLabel = new JLabel("LKR 0.00"), true));
        payCard.add(Box.createVerticalStrut(14));

        payCard.add(Box.createVerticalStrut(10));

        JButton checkoutBtn = UI.success("CHECKOUT  \u2713");
        checkoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        checkoutBtn.setFont(UIStyle.NORMAL_BOLD.deriveFont(16f));
        checkoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkoutBtn.addActionListener(e -> checkout());
        payCard.add(checkoutBtn);

        payCard.add(Box.createVerticalStrut(8));

        JButton clearBtn = UI.ghost("Clear Cart");
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> clearCart());
        payCard.add(clearBtn);

        right.add(payCard);
        right.add(Box.createVerticalGlue());

        add(left, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        refresh();
    }

    private JPanel totalsRow(String label, JLabel value, boolean big) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UIStyle.NORMAL_BOLD);
        l.setForeground(UIStyle.TEXT_MUTED);
        row.add(l, BorderLayout.WEST);
        value.setFont(big ? UIStyle.DISPLAY : UIStyle.NORMAL);
        value.setForeground(big ? UIStyle.PRIMARY : UIStyle.TEXT);
        value.setHorizontalAlignment(JLabel.RIGHT);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIStyle.SMALL_BOLD);
        l.setForeground(UIStyle.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void addByBarcode(String raw) {
        String barcode = raw == null ? "" : raw.trim();
        if (barcode.isEmpty()) return;
        Product p = productService.findByBarcode(barcode);
        if (p == null) {
            Toast.showError(owner, "No product found for barcode: " + barcode);
            scanField.setText("");
            return;
        }
        if (p.getStockQuantity() <= 0) {
            Toast.showError(owner, "'" + p.getName() + "' is out of stock.");
            scanField.setText("");
            return;
        }
        int inCart = 0;
        for (CartRow r : cart) if (r.product.getId() == p.getId()) inCart += r.qty;
        if (inCart + 1 > p.getStockQuantity()) {
            Toast.showError(owner, "Only " + p.getStockQuantity() + " in stock for '" + p.getName() + "'.");
            scanField.setText("");
            return;
        }
        addToCart(p, 1);
        scanField.setText("");
        scanField.requestFocusInWindow();
    }

    private void addToCart(Product p, int qty) {
        for (CartRow r : cart) {
            if (r.product.getId() == p.getId()) {
                r.qty += qty;
                updateCartTable();
                return;
            }
        }
        CartRow row = new CartRow();
        row.product = p;
        row.qty = qty;
        cart.add(row);
        updateCartTable();
    }

    private void updateCartTable() {
        cartModel.setRowCount(0);
        int qty = 0;
        double subtotal = 0;
        for (CartRow r : cart) {
            double amount = r.qty * r.product.getSellingPrice();
            cartModel.addRow(new Object[]{
                r.product.getName(), r.product.getBarcode(), r.qty,
                MONEY.format(r.product.getSellingPrice()), MONEY.format(amount)
            });
            qty += r.qty;
            subtotal += amount;
        }
        countLabel.setText(qty + " item" + (qty == 1 ? "" : "s"));
        subtotalLabel.setText("LKR " + MONEY.format(subtotal));
        totalLabel.setText("LKR " + MONEY.format(subtotal));
        discountField.setText("0");
    }

    private double currentSubtotal() {
        double s = 0;
        for (CartRow r : cart) s += r.qty * r.product.getSellingPrice();
        return s;
    }

    private void clearCart() {
        cart.clear();
        discountField.setText("0");
        customerCombo.setSelectedIndex(0);
        updateCartTable();
    }

    private void checkout() {
        if (cart.isEmpty()) {
            Toast.showInfo(owner, "Cart is empty. Scan or add items first.");
            return;
        }
        if (!auth.hasPermission(Permissions.POS)) {
            Toast.showError(owner, "You do not have permission to make sales.");
            return;
        }

        double subtotal = currentSubtotal();
        double discount = 0;
        try {
            discount = Double.parseDouble(discountField.getText().trim());
        } catch (NumberFormatException ex) {
            discount = 0;
        }
        if (discount < 0 || discount > subtotal) {
            Toast.showError(owner, "Discount must be between 0 and the subtotal.");
            return;
        }

        String[] methods = {"CASH", "CARD"};
        int pm = JOptionPane.showOptionDialog(this,
                "Payment method for " + MONEY.format(Math.max(0, subtotal - discount)) + "?",
                "Select payment method",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);
        if (pm < 0) return;
        String method = methods[pm];

        double totalAfterDiscount = Math.round((subtotal - discount) * 100.0) / 100.0;

        double paid = totalAfterDiscount;
        if ("CASH".equals(method)) {
            String input = JOptionPane.showInputDialog(this,
                    "Amount received from customer:\nTotal due: LKR " + MONEY.format(totalAfterDiscount),
                    "Cash Payment", JOptionPane.PLAIN_MESSAGE);
            if (input == null) return;
            try {
                paid = Double.parseDouble(input.trim());
            } catch (NumberFormatException ex) {
                Toast.showError(owner, "Invalid amount.");
                return;
            }
            if (paid < totalAfterDiscount) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "The amount received (LKR " + MONEY.format(paid) + ") is less than the total due.\n"
                        + "The sale will be recorded as short-paid. Continue?",
                        "Short payment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
        }

        List<SaleItem> items = new ArrayList<>();
        for (CartRow r : cart) {
            SaleItem si = new SaleItem();
            si.setProductId(r.product.getId());
            si.setProductName(r.product.getName());
            si.setBarcode(r.product.getBarcode());
            si.setQuantity(r.qty);
            items.add(si);
        }

        double taxRate = 0;
        try {
            taxRate = Double.parseDouble(Database.getInstance().getSetting("tax.rate", "0"));
        } catch (Exception ignored) {}

        Customer selected = (Customer) customerCombo.getSelectedItem();
        Integer customerId = selected == null ? null : selected.getId();

        try {
            Sale sale = saleService.completeSale(items, auth.getCurrentUser(), customerId,
                    discount, taxRate, method, paid);
            updateCartTable();

            int printChoice = JOptionPane.showConfirmDialog(this,
                    "Sale completed!  Invoice " + sale.getInvoiceNumber()
                    + "   Total: LKR " + MONEY.format(sale.getTotal())
                    + "\n\nPrint A4 bill now?", "Print bill",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (printChoice == JOptionPane.YES_OPTION) {
                printBill(sale, selected);
            }
            Toast.showSuccess(owner, "Sale complete - Invoice " + sale.getInvoiceNumber());
            clearCart();
        } catch (RuntimeException ex) {
            Toast.showError(owner, "Sale could not be completed: " + ex.getMessage());
        }
    }

    private void printBill(Sale sale, Customer customer) {
        User cashier = auth.getCurrentUser();
        try {
            BillPrinter printer = new BillPrinter(sale, customer, cashier);
            printer.print();
        } catch (RuntimeException ex) {
            Toast.showError(owner, "Printing failed: " + ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        Customer selected = (Customer) customerCombo.getSelectedItem();
        customerCombo.removeAllItems();
        customerCombo.addItem(null);
        for (Customer c : customerRepo.findAll()) customerCombo.addItem(c);
        if (selected != null) {
            for (int i = 0; i < customerCombo.getItemCount(); i++) {
                Customer o = customerCombo.getItemAt(i);
                if (o != null && o.getId() == selected.getId()) {
                    customerCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        scanField.requestFocusInWindow();
    }
}
