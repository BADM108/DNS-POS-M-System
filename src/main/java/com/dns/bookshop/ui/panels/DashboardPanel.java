package com.dns.bookshop.ui.panels;

import com.dns.bookshop.models.Sale;
import com.dns.bookshop.services.ReportService;
import com.dns.bookshop.theme.Icons;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Modern dashboard with clean metric cards and a recent-sales table.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private final ReportService report = new ReportService();
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private JLabel revenueVal, todaySalesVal, lowStockVal, productsVal, weekVal;
    private DefaultTableModel recentModel;

    public DashboardPanel(Window owner) {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(new javax.swing.border.EmptyBorder(20, 24, 20, 24));
        build();
    }

    private void build() {
        JPanel mainRow = new JPanel(new GridLayout(1, 2, 18, 0));
        mainRow.setOpaque(false);

        JPanel revenueCard = bigMetric("Today's Revenue", "LKR 0.00", UIStyle.PRIMARY, Icons.DASHBOARD);
        JPanel salesCard = bigMetric("Today's Sales", "0", UIStyle.INFO, Icons.SALES);
        revenueVal = valueLabel(revenueCard);
        todaySalesVal = valueLabel(salesCard);
        mainRow.add(revenueCard);
        mainRow.add(salesCard);

        JPanel grid = new JPanel(new GridLayout(1, 3, 18, 0));
        grid.setOpaque(false);
        JPanel low = metric("Low Stock", "0", UIStyle.WARNING, Icons.DASHBOARD);
        JPanel prod = metric("Total Products", "0", UIStyle.SUCCESS, Icons.PRODUCTS);
        JPanel week = metric("Weekly Revenue", "LKR 0.00", UIStyle.PRIMARY, Icons.REPORTS);
        lowStockVal = valueLabel(low);
        productsVal = valueLabel(prod);
        weekVal = valueLabel(week);
        grid.add(low);
        grid.add(prod);
        grid.add(week);

        JPanel recentPane = UI.card();
        recentPane.setLayout(new BorderLayout(0, 14));
        recentPane.add(UI.section("Recent Sales"), BorderLayout.NORTH);

        recentModel = new DefaultTableModel(new String[]{"Invoice", "Date", "Items", "Total", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(recentModel);
        UI.styleTableWithZebra(t);
        UI.alignColumn(t, 3, JLabel.RIGHT);
        recentPane.add(UI.table(t), BorderLayout.CENTER);

        JPanel centerStack = new JPanel(new BorderLayout(0, 18));
        centerStack.setOpaque(false);
        JPanel topBlock = new JPanel(new BorderLayout(0, 18));
        topBlock.setOpaque(false);
        topBlock.add(mainRow, BorderLayout.NORTH);
        topBlock.add(grid, BorderLayout.CENTER);
        centerStack.add(topBlock, BorderLayout.NORTH);
        centerStack.add(recentPane, BorderLayout.CENTER);

        add(centerStack, BorderLayout.CENTER);

        refresh();
    }

    private JPanel bigMetric(String label, String value, Color accent, String glyph) {
        JPanel card = UI.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(iconChip(glyph, accent, Component.LEFT_ALIGNMENT));
        card.add(Box.createVerticalStrut(14));
        card.add(cardLabel(label.toUpperCase(), UIStyle.SMALL_BOLD, UIStyle.TEXT_MUTED, Component.LEFT_ALIGNMENT));
        card.add(Box.createVerticalStrut(6));
        JLabel val = cardLabel(value, new Font("Segoe UI", Font.BOLD, 28), accent, Component.LEFT_ALIGNMENT);
        card.add(val);
        return card;
    }

    private JPanel metric(String label, String value, Color accent, String glyph) {
        JPanel card = UI.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(iconChip(glyph, accent, Component.LEFT_ALIGNMENT));
        card.add(Box.createVerticalStrut(12));
        card.add(cardLabel(label.toUpperCase(), UIStyle.SMALL_BOLD, UIStyle.TEXT_MUTED, Component.LEFT_ALIGNMENT));
        card.add(Box.createVerticalStrut(4));
        JLabel val = cardLabel(value, UIStyle.PAGE_TITLE, accent, Component.LEFT_ALIGNMENT);
        card.add(val);
        return card;
    }

    /** A small rounded accent chip holding a white glyph icon. */
    private JComponent iconChip(String glyph, Color accent, float alignment) {
        return UI.metricIconChip(glyph, accent, alignment);
    }

    private JLabel cardLabel(String text, Font f, Color c, float alignment) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(alignment);
        return l;
    }

    private JLabel valueLabel(JPanel card) {
        JLabel last = null;
        for (Component c : card.getComponents()) {
            if (c instanceof JLabel) last = (JLabel) c;
        }
        return last;
    }

    @Override
    public void refresh() {
        Map<String, Object> today = report.summary(report.startOfToday(), System.currentTimeMillis());
        Map<String, Object> weekly = report.summary(report.startOfLast7Days(), System.currentTimeMillis());

        revenueVal.setText("LKR " + MONEY.format(today.get("revenue")));
        todaySalesVal.setText(String.valueOf(today.get("sales")));
        lowStockVal.setText(String.valueOf(today.get("lowStock")));
        productsVal.setText(String.valueOf(today.get("products")));
        weekVal.setText("LKR " + MONEY.format(weekly.get("revenue")));

        recentModel.setRowCount(0);
        for (Sale s : report.recentSales(12)) {
            String date = DateTimeFormatter.ofPattern("dd MMM HH:mm")
                    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(s.getCreatedAt()));
            recentModel.addRow(new Object[]{
                s.getInvoiceNumber(), date, s.getItems().size(),
                MONEY.format(s.getTotal()), s.getStatus()
            });
        }
    }
}
