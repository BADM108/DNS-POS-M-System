package com.dns.bookshop.ui.panels;

import com.dns.bookshop.models.Sale;
import com.dns.bookshop.services.ReportService;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DashboardPanel extends JPanel implements Refreshable {

    private final ReportService report = new ReportService();
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private JLabel revenueVal, todaySalesVal, lowStockVal, productsVal, weekVal, custVal;
    private DefaultTableModel recentModel;

    public DashboardPanel() {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        build();
    }

    private void build() {
        JPanel topLeft = new JPanel(new GridLayout(1, 2, 16, 0));
        topLeft.setOpaque(false);

        JPanel revenueCard = kpi("Today's Revenue");
        JPanel salesCard = kpi("Today's Sales Count");
        revenueVal = (JLabel) revenueCard.getComponent(2);
        todaySalesVal = (JLabel) salesCard.getComponent(2);

        JPanel row = new JPanel(new GridLayout(1, 2, 16, 16));
        row.setOpaque(false);
        row.add(revenueCard);
        row.add(salesCard);
        topLeft.add(row);
        topLeft.add(emptyCard("", ""));

        JPanel statsTop = new JPanel(new BorderLayout());
        statsTop.setOpaque(false);
        statsTop.add(topLeft, BorderLayout.CENTER);
        add(statsTop, BorderLayout.NORTH);

        // Lower KPI grid
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 12));
        grid.setOpaque(false);
        grid.add(cardBuilder("Low Stock Items", "0", UIStyle.WARNING));
        grid.add(cardBuilder("Total Products", "0", UIStyle.ACCENT));
        grid.add(cardBuilder("Weekly Revenue", "LKR 0.00", UIStyle.SUCCESS));
        grid.add(cardBuilder("Total Customers", "0", UIStyle.PRIMARY));
        lowStockVal = (JLabel) ((JPanel) grid.getComponent(0)).getComponent(2);
        productsVal = (JLabel) ((JPanel) grid.getComponent(1)).getComponent(2);
        weekVal = (JLabel) ((JPanel) grid.getComponent(2)).getComponent(2);
        custVal = (JLabel) ((JPanel) grid.getComponent(3)).getComponent(2);
        add(grid, BorderLayout.CENTER);

        JPanel recentPane = UI.card();
        recentPane.setLayout(new BorderLayout());
        JPanel hdrRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hdrRow.setOpaque(false);
        hdrRow.add(UI.section("Recent Sales"));
        recentPane.add(hdrRow, BorderLayout.NORTH);

        recentModel = new DefaultTableModel(new String[]{"Invoice", "Date", "Items", "Total", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(recentModel);
        recentPane.add(UI.table(t), BorderLayout.CENTER);
        add(recentPane, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel kpi(String name) {
        JPanel card = UI.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(label(name, UIStyle.SMALL, UIStyle.MUTED));
        card.add(Box.createVerticalStrut(8));
        card.add(label("0", UIStyle.TITLE, UIStyle.PRIMARY));
        return card;
    }

    private JPanel cardBuilder(String name, String value, Color accent) {
        JPanel card = UI.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(label(name, UIStyle.SMALL, UIStyle.MUTED));
        card.add(Box.createVerticalStrut(8));
        card.add(label(value, UIStyle.TITLE, accent));
        return card;
    }

    private JPanel emptyCard(String a, String b) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    private JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(CENTER_ALIGNMENT);
        l.setHorizontalAlignment(JLabel.CENTER);
        return l;
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
        custVal.setText(String.valueOf(today.get("customers")));

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
