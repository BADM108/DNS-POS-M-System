package com.dns.bookshop.ui.panels;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.db.Database;
import com.dns.bookshop.theme.Toast;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.theme.UIStyle;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * Shop settings stored locally. Admin-only by convention.
 */
public class SettingsPanel extends JPanel implements Refreshable {

    private final java.awt.Window owner;
    private JTextField shopName, taxRate, invoicePrefix;

    public SettingsPanel(java.awt.Window owner) {
        super(new BorderLayout());
        this.owner = owner;
        setBackground(UIStyle.BG);
        setBorder(new javax.swing.border.EmptyBorder(16, 20, 16, 20));
        build();
    }

    private void build() {
        JPanel card = UI.card();
        card.setLayout(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(560, 320));
        card.add(UI.section("Shop Configuration"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);

        form.add(UI.label("Shop name:"));
        shopName = UI.textField();
        form.add(shopName);

        form.add(UI.label("Tax rate (%):"));
        taxRate = UI.textField();
        form.add(taxRate);

        form.add(UI.label("Invoice prefix:"));
        invoicePrefix = UI.textField();
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
            Toast.showSuccess(owner, "Settings saved.");
        } catch (NumberFormatException ex) {
            Toast.showError(owner, "Tax rate must be a number between 0 and 100.");
        }
    }

    @Override
    public void refresh() {
        Database db = Database.getInstance();
        shopName.setText(db.getSetting("shop.name", AppConfig.SHOP_NAME));
        taxRate.setText(db.getSetting("tax.rate", "0"));
        invoicePrefix.setText(db.getSetting("invoice.prefix", "DNS"));
    }
}
