package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.repositories.CustomerRepository;
import com.dns.bookshop.db.repositories.SaleRepository;
import com.dns.bookshop.db.repositories.UserRepository;
import com.dns.bookshop.models.Customer;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.Sale;
import com.dns.bookshop.models.SaleItem;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.services.SaleService;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.util.BillPrinter;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Sales history with search, item details, refund and reprint bill.
 */
public class SalesPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final SaleRepository saleRepo = new SaleRepository();
    private final SaleService saleService = new SaleService();
    private final java.awt.Window owner;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private JComboBox<String> rangeCombo;
    private JTable table, itemsTable;
    private DefaultTableModel model, itemsModel;

    public SalesPanel(java.awt.Window owner) {
        super(new BorderLayout());
        this.owner = owner;
        setBackground(UIStyle.BG);
        setBorder(new javax.swing.border.EmptyBorder(16, 20, 16, 20));
        build();
    }

    private void build() {
        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);

        JPanel controls = new JPanel(new BorderLayout(8, 0));
        controls.setOpaque(false);
        rangeCombo = new JComboBox<>(new String[]{"Today", "Last 7 days", "This month", "All time"});
        rangeCombo.addActionListener(e -> loadSales());
        rangeCombo.setPreferredSize(new Dimension(160, 38));
        controls.add(rangeCombo, BorderLayout.CENTER);
        JButton refreshBtn = UI.ghost("Refresh");
        refreshBtn.addActionListener(e -> loadSales());
        controls.add(refreshBtn, BorderLayout.WEST);
        top.add(controls, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSalesList(), buildDetails());
        split.setResizeWeight(0.6);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        loadSales();
    }

    private JPanel buildSalesList() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(UI.section("Sales"), BorderLayout.NORTH);
        model = new DefaultTableModel(new String[]{"Invoice", "Date", "Cashier", "Total", "Method", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) loadItemsForSelected();
        });
        UI.styleTableWithZebra(table);
        UI.alignColumn(table, 3, JLabel.RIGHT);
        p.add(UI.table(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildDetails() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(UI.section("Sale Items"), BorderLayout.NORTH);
        itemsModel = new DefaultTableModel(new String[]{"Item", "Barcode", "Qty", "Price", "Amount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        itemsTable = new JTable(itemsModel);
        UI.styleTableWithZebra(itemsTable);
        for (int c : new int[]{2, 3, 4}) UI.alignColumn(itemsTable, c, JLabel.RIGHT);
        p.add(UI.table(itemsTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
        actions.setOpaque(false);
        JButton reprint = UI.ghost("Reprint Bill");
        reprint.addActionListener(e -> reprintBill());
        JButton refund = UI.danger("Refund Sale");
        refund.addActionListener(e -> refundSale());
        JButton details = UI.ghost("Receipt Details");
        details.addActionListener(e -> showDetails());
        actions.add(reprint);
        actions.add(refund);
        actions.add(details);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private void loadSales() {
        long start = 0, end = System.currentTimeMillis();
        String sel = (String) rangeCombo.getSelectedItem();
        if ("Today".equals(sel)) {
            start = startOfDay();
        } else if ("Last 7 days".equals(sel)) {
            start = startOfDay() - 6L * 86400000L;
        } else if ("This month".equals(sel)) {
            start = startOfMonth();
        }
        model.setRowCount(0);
        List<Sale> sales = start == 0 && "All time".equals(sel)
                ? saleRepo.findBetween(0L, end)
                : saleRepo.findBetween(start, end);
        UserRepository userRepo = new UserRepository();
        for (Sale s : sales) {
            String date = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
                    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(s.getCreatedAt()));
            User u = userRepo.findById(s.getUserId());
            model.addRow(new Object[]{
                s.getInvoiceNumber(), date,
                u == null ? "#" + s.getUserId() : u.getFullName(),
                MONEY.format(s.getTotal()), s.getPaymentMethod(), s.getStatus()
            });
        }
        itemsModel.setRowCount(0);
    }

    private long startOfDay() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        return now.toLocalDate().atStartOfDay(now.getZone()).toInstant().toEpochMilli();
    }

    private long startOfMonth() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        return now.toLocalDate().withDayOfMonth(1).atStartOfDay(now.getZone()).toInstant().toEpochMilli();
    }

    private Sale selectedSale() {
        int r = table.getSelectedRow();
        if (r < 0) return null;
        String invoice = (String) model.getValueAt(r, 0);
        return saleRepo.findByInvoice(invoice);
    }

    private void loadItemsForSelected() {
        Sale s = selectedSale();
        itemsModel.setRowCount(0);
        if (s == null) return;
        for (SaleItem it : s.getItems()) {
            itemsModel.addRow(new Object[]{
                it.getProductName(), it.getBarcode(), it.getQuantity(),
                MONEY.format(it.getUnitPrice()), MONEY.format(it.getLineTotal())
            });
        }
    }

    private void reprintBill() {
        Sale s = selectedSale();
        if (s == null) {
            Toast.showInfo(owner, "Select a sale first.");
            return;
        }
        Customer c = s.getCustomerId() != null ? new CustomerRepository().findById(s.getCustomerId()) : null;
        User u = new UserRepository().findById(s.getUserId());
        try {
            new BillPrinter(s, c, u).print();
        } catch (RuntimeException ex) {
            Toast.showError(owner, "Printing failed: " + ex.getMessage());
        }
    }

    private void refundSale() {
        Sale s = selectedSale();
        if (s == null) {
            Toast.showInfo(owner, "Select a sale first.");
            return;
        }
        if (!auth.hasPermission(Permissions.REFUND_SALES)) {
            Toast.showError(owner, "You do not have permission to refund sales.");
            return;
        }
        if (Sale.STATUS_REFUNDED.equals(s.getStatus())) {
            Toast.showWarning(owner, "This sale has already been refunded.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Refund invoice " + s.getInvoiceNumber() + " (LKR " + MONEY.format(s.getTotal()) + ")?\n"
                + "Stock will be returned to inventory.",
                "Confirm refund", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                saleService.refund(s.getId(), auth.getCurrentUser());
                Toast.showSuccess(owner, "Sale refunded. Stock returned.");
                loadSales();
            } catch (RuntimeException ex) {
                Toast.showError(owner, ex.getMessage());
            }
        }
    }

    private void showDetails() {
        Sale s = selectedSale();
        if (s == null) return;
        Customer c = s.getCustomerId() != null ? new CustomerRepository().findById(s.getCustomerId()) : null;
        String msg = "Invoice:      " + s.getInvoiceNumber() +
                "\nDate:         " + DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss")
                    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(s.getCreatedAt())) +
                "\nCustomer:     " + (c == null ? "Walk-in" : c.getName()) +
                "\nSubtotal:     LKR " + MONEY.format(s.getSubtotal()) +
                "\nDiscount:     LKR " + MONEY.format(s.getDiscount()) +
                "\nTax:          LKR " + MONEY.format(s.getTax()) +
                "\nTOTAL:        LKR " + MONEY.format(s.getTotal()) +
                "\nPaid:         LKR " + MONEY.format(s.getAmountPaid()) +
                "\nChange:       LKR " + MONEY.format(s.getChangeGiven()) +
                "\nMethod:       " + s.getPaymentMethod() +
                "\nStatus:       " + s.getStatus();
        JOptionPane.showMessageDialog(this, msg, "Receipt Details", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void refresh() {
        loadSales();
    }
}
