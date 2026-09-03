package com.dns.bookshop.ui.panels;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.services.BackupService;
import com.dns.bookshop.services.ProductService;
import com.dns.bookshop.services.ReportService;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Reports & analytics: revenue summary, top products, and data export/backup.
 */
public class ReportPanel extends JPanel implements Refreshable {

    private final AuthService auth = AuthService.getInstance();
    private final ReportService report = new ReportService();
    private final BackupService backup = new BackupService();
    private final java.awt.Window owner;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private JLabel revAmt, salesCnt, prodCnt, custCnt;
    private DefaultTableModel model;

    public ReportPanel(java.awt.Window owner) {
        super(new BorderLayout());
        this.owner = owner;
        setBackground(UIStyle.BG);
        setBorder(new javax.swing.border.EmptyBorder(16, 20, 16, 20));
        build();
    }

    private void build() {
        JPanel kpis = new JPanel(new GridLayout(1, 4, 16, 0));
        kpis.setOpaque(false);
        JPanel s = metric("Sales This Month", "0", UIStyle.PRIMARY);
        JPanel r = metric("Revenue This Month", "LKR 0.00", UIStyle.SUCCESS);
        JPanel p = metric("Total Products", "0", UIStyle.INFO);
        JPanel c = metric("Total Customers", "0", UIStyle.WARNING);
        salesCnt = valueLabel(s);
        revAmt = valueLabel(r);
        prodCnt = valueLabel(p);
        custCnt = valueLabel(c);
        kpis.add(s);
        kpis.add(r);
        kpis.add(p);
        kpis.add(c);
        add(kpis, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTopProducts(), buildActions());
        split.setResizeWeight(0.65);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private JPanel metric(String label, String value, java.awt.Color accent) {
        JPanel card = UI.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel n = new JLabel(label);
        n.setFont(UIStyle.SMALL_BOLD);
        n.setForeground(UIStyle.TEXT_MUTED);
        n.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel v = new JLabel(value);
        v.setFont(UIStyle.TITLE);
        v.setForeground(accent);
        v.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(n);
        card.add(Box.createVerticalStrut(8));
        card.add(v);
        return card;
    }

    private JLabel valueLabel(JPanel card) {
        Component[] comps = card.getComponents();
        for (int i = comps.length - 1; i >= 0; i--) {
            if (comps[i] instanceof JLabel && ((JLabel) comps[i]).getFont().getSize() >= 22) {
                return (JLabel) comps[i];
            }
        }
        return null;
    }

    private JPanel buildTopProducts() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(UI.section("Top Products (Last 30 days)"), BorderLayout.NORTH);
        model = new DefaultTableModel(new String[]{"#", "Product", "Category", "Units Sold", "Selling Price"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(model);
        UI.styleTableWithZebra(t);
        UI.alignColumn(t, 3, JLabel.RIGHT);
        UI.alignColumn(t, 4, JLabel.RIGHT);
        p.add(UI.table(t), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildActions() {
        JPanel card = UI.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(UI.section("Data & Backup"));
        card.add(Box.createVerticalStrut(12));
        card.add(UI.label("All data is stored locally on this PC."));
        card.add(Box.createVerticalStrut(16));

        JButton db = UI.primary("Backup Database (.db)");
        db.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        db.setAlignmentX(Component.CENTER_ALIGNMENT);
        db.addActionListener(e -> backupDb());
        card.add(db);

        card.add(Box.createVerticalStrut(8));
        JButton csvToday = UI.ghost("Export Today's Sales (CSV)");
        csvToday.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        csvToday.setAlignmentX(Component.CENTER_ALIGNMENT);
        csvToday.addActionListener(e -> exportCsv(report.startOfToday(), System.currentTimeMillis()));
        card.add(csvToday);

        card.add(Box.createVerticalStrut(8));
        JButton csvMonth = UI.ghost("Export This Month (CSV)");
        csvMonth.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        csvMonth.setAlignmentX(Component.CENTER_ALIGNMENT);
        csvMonth.addActionListener(e -> exportCsv(report.startOfMonth(), System.currentTimeMillis()));
        card.add(csvMonth);

        card.add(Box.createVerticalStrut(20));
        JLabel path = new JLabel("<html><small>Data folder:<br>" +
                AppConfig.dataDir().toAbsolutePath() + "</small></html>");
        path.setFont(UIStyle.SMALL);
        path.setForeground(UIStyle.TEXT_MUTED);
        card.add(path);

        card.add(Box.createVerticalGlue());
        return card;
    }

    private void backupDb() {
        if (!auth.hasPermission(Permissions.BACKUP_DATA)) {
            Toast.showError(owner, "No permission to back up data.");
            return;
        }
        try {
            File f = backup.exportDatabaseBackup();
            Toast.showSuccess(owner, "Database backed up to:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            Toast.showError(owner, "Backup failed: " + ex.getMessage());
        }
    }

    private void exportCsv(long start, long end) {
        if (!auth.hasPermission(Permissions.BACKUP_DATA)) {
            Toast.showError(owner, "No permission to export data.");
            return;
        }
        try {
            File f = backup.exportSalesCsv(start, end);
            Toast.showSuccess(owner, "Exported to:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            Toast.showError(owner, "Export failed: " + ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        long now = System.currentTimeMillis();
        Map<String, Object> s = report.summary(report.startOfMonth(), now);
        salesCnt.setText(String.valueOf(s.get("sales")));
        revAmt.setText("LKR " + MONEY.format(s.get("revenue")));
        prodCnt.setText(String.valueOf(s.get("products")));
        custCnt.setText(String.valueOf(s.get("customers")));

        model.setRowCount(0);
        int n = 1;
        List<Product> top = report.topProducts(report.startOfMonth() - 30L * 86400000L, now, 20);
        for (Product p : top) {
            int qty = report.topProductQuantities(report.startOfMonth() - 30L * 86400000L, now, 50)
                    .getOrDefault(p.getId(), 0);
            model.addRow(new Object[]{n++, p.getName(),
                    ProductService.formatCategory(p.getCategory()), qty,
                    MONEY.format(p.getSellingPrice())});
        }
    }
}
