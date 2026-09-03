package com.dns.bookshop;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.db.Database;
import com.dns.bookshop.db.repositories.UserRepository;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.services.ProductService;
import com.dns.bookshop.util.PasswordUtil;
import com.dns.bookshop.ui.panels.CustomerPanel;
import com.dns.bookshop.ui.panels.DashboardPanel;
import com.dns.bookshop.ui.panels.PosPanel;
import com.dns.bookshop.ui.panels.ProductPanel;
import com.dns.bookshop.ui.panels.ReportPanel;
import com.dns.bookshop.ui.panels.SalesPanel;
import com.dns.bookshop.ui.panels.SettingsPanel;
import com.dns.bookshop.ui.panels.UsersPanel;

import com.dns.bookshop.models.Product;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Headless smoke test: builds every UI panel to catch construction errors.
 */
public class UiSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path tmp = Files.createTempDirectory("dns-ui-test");
        AppConfig.setDataDir(tmp);
        Database.getInstance().init();

        // Seed a product and a worker so panels have data.
        AuthService.getInstance().login("admin", "admin");
        ProductService ps = new ProductService();
        Product book = new Product();
        book.setName("Smoke Book");
        book.setCategory("BOOK");
        book.setSellingPrice(500);
        book.setPurchasePrice(300);
        book.setStockQuantity(5);
        ps.register(book, true, true);

        UserRepository ur = new UserRepository();
        User w = new User("worker", PasswordUtil.hash("pass1234"), "W", "WORKER");
        ur.create(w);
        ur.savePermissions(w.getId(), FunctionalTest.defaultWorkerPerms());

        int fails = 0;
        fails += build("CustomerPanel", () -> new CustomerPanel(null));
        fails += build("DashboardPanel", () -> new DashboardPanel(null));
        fails += build("PosPanel", () -> new PosPanel(null));
        fails += build("ProductPanel", () -> new ProductPanel(null));
        fails += build("ReportPanel", () -> new ReportPanel(null));
        fails += build("SalesPanel", () -> new SalesPanel(null));
        fails += build("SettingsPanel", () -> new SettingsPanel(null));
        fails += build("UsersPanel", () -> new UsersPanel(null));

        System.out.println("== UI Smoke: " + fails + " failure(s) ==");
        if (fails > 0) System.exit(1);
        else System.out.println("ALL UI PANELS BUILT");
    }

    interface Builder { javax.swing.JComponent build(); }

    static int build(String name, Builder b) {
        try {
            javax.swing.JComponent c = b.build();
            c.getPreferredSize();
            System.out.println("OK   - " + name);
            return 0;
        } catch (Throwable t) {
            System.out.println("FAIL - " + name + ": " + t);
            t.printStackTrace();
            return 1;
        }
    }
}
