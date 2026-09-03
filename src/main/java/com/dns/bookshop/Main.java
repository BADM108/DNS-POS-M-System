package com.dns.bookshop;

import com.dns.bookshop.db.Database;
import com.dns.bookshop.theme.UI;
import com.dns.bookshop.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UI.applyGlobalTheme();
        } catch (Exception ignored) {
        }

        Database.getInstance().init();

        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }
}
