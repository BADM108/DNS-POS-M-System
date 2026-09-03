package com.dns.bookshop.ui.panels;

import com.dns.bookshop.db.Database;
import com.dns.bookshop.theme.UIStyle;
import com.dns.bookshop.theme.UI;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * Shop settings stored locally. Admin-only by convention.
 */
public class SettingsPanel extends JPanel implements Refreshable {

    private JTextField shopName, taxRate, invoicePrefix;

    public SettingsPanel() {
        super(new BorderLayout());
        setBackground(UIStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        build();
    }

    private void build() {
        add(UI.title("Shop Settings"), BorderLayout.NORTH);

        JPanel card = UI.card();
        card.setLayout(new BorderLayout(0, 14));
        card.add(UI.section("DNS BookShop Configuration"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);

        form.add(UI.label("Shop name:"));
        shopName = new JTextField();
        form.add(shopName);

        form.add(UI.label("Tax rate (%):"));
        taxRate = new JTextField();
        form.add(taxRate);

        form.add(UI.label("Invoice prefix:"));
        invoicePrefix = new JTextField();
        form.add(invoicePrefix);

        card.add(form, BorderLayout.CENTER);

        JButton save = UI.primary("Save Settings");
        save.addActionListener(e -> save());
        JPanel btnRow = new JPanel(new BorderLayout());
        btnRow.setOpaque(false);
        btnRow.add(new JLabel(" "), BorderLayout.CENTER);
        btnRow.add(save, BorderLayout.EAST);
        card.add(btnRow, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.NORTH);
        add(wrap, BorderLayout.CENTER);

        refresh();
    }

    private void save() {
        Database db = Database.getInstance();
        try {
            double t = Double.parseDouble(taxRate.getText().trim());
            if (t < 0 || t > 100) throw new NumberFormatException();
            db.setSetting("shop.name", shopName.getText().trim());
            db.setSetting("tax.rate", String.valueOf(t));
            db.setSetting("invoice.prefix", invoicePrefix.getText().trim().isEmpty() ? "DNS" : invoicePrefix.getText().trim());
            JOptionPane.showMessageDialog(this, "Settings saved.", "Done",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Tax rate must be a number between 0 and 100.",
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        Database db = Database.getInstance();
        shopName.setText(db.getSetting("shop.name", com.dns.bookshop.config.AppConfig.SHOP_NAME));
        taxRate.setText(db.getSetting("tax.rate", "0"));
        invoicePrefix.setText(db.getSetting("invoice.prefix", "DNS"));
    }
}
