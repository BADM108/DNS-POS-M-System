package com.dns.bookshop.ui.panels;

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
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.util.BillPrinter;
import com.dns.bookshop.util.ScannerInput;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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

    public PosPanel() {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        build();
    }

    private void build() {
        // === Left: cart ===
        JPanel left = UI.card();
        left.setLayout(new BorderLayout());

        JPanel topBlock = new JPanel(new BorderLayout());
        topBlock.setOpaque(false);
        JPanel headerLeft = new JPanel(new GridLayout(1, 2));
        headerLeft.setOpaque(false);
        headerLeft.add(new JLabel("Scan or type barcode"));
        JLabel hint = new JLabel("Scanner ready");
        hint.setFont(UIStyle.SMALL);
        hint.setForeground(UIStyle.MUTED);
        hint.setHorizontalAlignment(JLabel.RIGHT);
        headerLeft.add(hint);

        JPanel scanRow = new JPanel(new BorderLayout(8, 0));
        scanRow.setOpaque(false);
        scanRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        scanField = new JTextField();
        scanField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        JButton addBtn = UI.primary("Add");
        addBtn.addActionListener(e -> addByBarcode(scanField.getText()));
        scanRow.add(scanField, BorderLayout.CENTER);
        scanRow.add(addBtn, BorderLayout.EAST);

        topBlock.add(headerLeft, BorderLayout.NORTH);
        topBlock.add(scanRow, BorderLayout.CENTER);
        left.add(topBlock, BorderLayout.NORTH);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        cartModel = new DefaultTableModel(new String[]{"Item", "Barcode", "Qty", "Price", "Amount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        JScrollPane sp = UI.table(cartTable);
        sp.setPreferredSize(new Dimension(640, 420));
        tableWrap.add(sp, BorderLayout.CENTER);
        left.add(tableWrap, BorderLayout.CENTER);

        // Capture the scan into the cart.
        ScannerInput.attach(scanField, this::addByBarcode);

        // === Right: totals & payment ===
        JPanel right = UI.card();
        right.setPreferredSize(new Dimension(340, 0));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        right.add(UI.section("Payment"));
        right.add(Box.createVerticalStrut(8));

        right.add(UI.label("Customer"));
        customerCombo = new JComboBox<>();
        customerCombo.addItem(null);
        for (Customer c : customerRepo.findAll()) customerCombo.addItem(c);
        customerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        right.add(customerCombo);
        right.add(Box.createVerticalStrut(10));

        right.add(UI.label("Discount (LKR)"));
        discountField = new JTextField("0");
        right.add(discountField);

        right.add(Box.createVerticalStrut(6));
        subtotalLabel = new JLabel("LKR 0.00");
        subtotalLabel.setFont(UIStyle.NORMAL);
        totalLabel = new JLabel("LKR 0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        totalLabel.setForeground(UIStyle.PRIMARY);
        countLabel = new JLabel("0 items");
        countLabel.setFont(UIStyle.SMALL);
        countLabel.setForeground(UIStyle.MUTED);

        right.add(countLabel);
        right.add(Box.createVerticalStrut(6));
        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        subRow.setOpaque(false);
        subRow.add(new JLabel("Subtotal:  "));
        subRow.add(subtotalLabel);
        right.add(subRow);
        JPanel totalRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalRow.setOpaque(false);
        totalRow.add(new JLabel("TOTAL:  "));
        totalRow.add(totalLabel);
        right.add(totalRow);

        right.add(Box.createVerticalGlue());

        JButton checkoutBtn = UI.success("CHECKOUT");
        checkoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        checkoutBtn.addActionListener(e -> checkout());
        right.add(checkoutBtn);

        JButton clearBtn = UI.ghost("Clear Cart");
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        clearBtn.addActionListener(e -> clearCart());
        right.add(Box.createVerticalStrut(8));
        right.add(clearBtn);

        add(left, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        refresh();
    }

    private void addByBarcode(String raw) {
        String barcode = raw == null ? "" : raw.trim();
        if (barcode.isEmpty()) return;
        Product p = productService.findByBarcode(barcode);
        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "No product found for barcode: " + barcode +
                    "\n\nRegister this item first under Products & Barcodes.",
                    "Not found", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (p.getStockQuantity() <= 0) {
            JOptionPane.showMessageDialog(this, "'" + p.getName() + "' is out of stock.",
                    "Out of stock", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Validate stock vs cart qty
        int inCart = 0;
        for (CartRow r : cart) if (r.product.getId() == p.getId()) inCart += r.qty;
        if (inCart + 1 > p.getStockQuantity()) {
            JOptionPane.showMessageDialog(this, "Only " + p.getStockQuantity() + " in stock for '"
                    + p.getName() + "'.", "Insufficient stock", JOptionPane.WARNING_MESSAGE);
            return;
        }
        addToCart(p, 1);
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
        countLabel.setText(qty + " items");
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
            JOptionPane.showMessageDialog(this, "Cart is empty. Scan or add items first.",
                    "Nothing to checkout", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!auth.hasPermission(Permissions.POS)) {
            JOptionPane.showMessageDialog(this, "You do not have permission to make sales.",
                    "Permission denied", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Discount must be between 0 and the subtotal.",
                    "Invalid discount", JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
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
            taxRate = Double.parseDouble(com.dns.bookshop.db.Database.getInstance().getSetting("tax.rate", "0"));
        } catch (Exception ignored) {}

        Customer selected = (Customer) customerCombo.getSelectedItem();
        Integer customerId = selected == null ? null : selected.getId();

        try {
            Sale sale = saleService.completeSale(items, auth.getCurrentUser(), customerId,
                    discount, taxRate, method, paid);
            updateCartTable();

            int printChoice = JOptionPane.showConfirmDialog(this,
                    "Sale completed! Invoice " + sale.getInvoiceNumber()
                    + "  Total: LKR " + MONEY.format(sale.getTotal())
                    + "\n\nPrint A4 bill now?", "Print bill",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (printChoice == JOptionPane.YES_OPTION) {
                printBill(sale, selected);
            }
            clearCart();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Sale could not be completed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printBill(Sale sale, Customer customer) {
        User cashier = auth.getCurrentUser();
        try {
            BillPrinter printer = new BillPrinter(sale, customer, cashier);
            printer.print();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage(),
                    "Print error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        // Refresh customer dropdown when switching to the panel.
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
