package com.dns.bookshop.ui.panels;

import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.services.BackupService;
import com.dns.bookshop.services.ProductService;
import com.dns.bookshop.services.ReportService;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
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
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private JLabel revAmt, salesCnt, prodCnt, custCnt;
    private DefaultTableModel model;

    public ReportPanel() {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        build();
    }

    private void build() {
        add(UI.title("Reports & Analytics"), BorderLayout.NORTH);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 16, 0));
        kpis.setOpaque(false);
        kpis.add(kpi("Sales This Month", UIStyle.PRIMARY));
        kpis.add(kpi("Revenue This Month", UIStyle.SUCCESS));
        kpis.add(kpi("Total Products", UIStyle.ACCENT));
        kpis.add(kpi("Total Customers", UIStyle.WARNING));
        salesCnt = (JLabel) ((JPanel) kpis.getComponent(0)).getComponent(2);
        revAmt = (JLabel) ((JPanel) kpis.getComponent(1)).getComponent(2);
        prodCnt = (JLabel) ((JPanel) kpis.getComponent(2)).getComponent(2);
        custCnt = (JLabel) ((JPanel) kpis.getComponent(3)).getComponent(2);
        add(kpis, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTopProducts(), buildActions());
        split.setResizeWeight(0.65);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);
    }

    private JPanel kpi(String name, java.awt.Color accent) {
        JPanel card = UI.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel n = new JLabel(name);
        n.setFont(UIStyle.SMALL);
        n.setForeground(UIStyle.MUTED);
        n.setAlignmentX(CENTER_ALIGNMENT);
        JLabel v = new JLabel("0");
        v.setFont(new Font("SansSerif", Font.BOLD, 22));
        v.setForeground(accent);
        v.setAlignmentX(CENTER_ALIGNMENT);
        card.add(n);
        card.add(Box.createVerticalStrut(8));
        card.add(v);
        return card;
    }

    private JPanel buildTopProducts() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(UI.section("Top Products (Last 30 days)"), BorderLayout.NORTH);
        model = new DefaultTableModel(new String[]{"#", "Product", "Category", "Units Sold", "Selling Price"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(model);
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
        db.addActionListener(e -> backupDb());
        card.add(db);

        card.add(Box.createVerticalStrut(8));
        JButton csvToday = UI.ghost("Export Today's Sales (CSV)");
        csvToday.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        csvToday.addActionListener(e -> exportCsv(report.startOfToday(), System.currentTimeMillis()));
        card.add(csvToday);

        card.add(Box.createVerticalStrut(8));
        JButton csvMonth = UI.ghost("Export This Month (CSV)");
        csvMonth.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        csvMonth.addActionListener(e -> exportCsv(report.startOfMonth(), System.currentTimeMillis()));
        card.add(csvMonth);

        card.add(Box.createVerticalStrut(20));
        JLabel path = new JLabel("<html><small>Data folder:<br>" +
                com.dns.bookshop.config.AppConfig.dataDir().toAbsolutePath() + "</small></html>");
        path.setFont(UIStyle.SMALL);
        path.setForeground(UIStyle.MUTED);
        card.add(path);

        card.add(Box.createVerticalGlue());
        return card;
    }

    private void backupDb() {
        if (!auth.hasPermission(Permissions.BACKUP_DATA)) {
            JOptionPane.showMessageDialog(this, "No permission to back up data.",
                    "Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            File f = backup.exportDatabaseBackup();
            JOptionPane.showMessageDialog(this,
                    "Database backed up to:\n" + f.getAbsolutePath(),
                    "Backup complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv(long start, long end) {
        if (!auth.hasPermission(Permissions.BACKUP_DATA)) {
            JOptionPane.showMessageDialog(this, "No permission to export data.",
                    "Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            File f = backup.exportSalesCsv(start, end);
            JOptionPane.showMessageDialog(this,
                    "Exported to:\n" + f.getAbsolutePath(),
                    "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
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
