package com.dns.bookshop;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.db.Database;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.services.AuthService;
import com.dns.bookshop.ui.panels.UsersPanel;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifies the authorization model that drives the navigation:
 * admins see everything, default workers have a defined subset without
 * add-customers / manage-users / delete-products.
 */
public class MainFrameTest {
    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("dns-mf-test");
        AppConfig.setDataDir(tmp);
        Database.getInstance().init();

        AuthService auth = AuthService.getInstance();

        // Admin
        auth.login("admin", "admin");
        boolean adminAll = auth.hasPermission(Permissions.MANAGE_USERS)
                && auth.hasPermission(Permissions.ADD_CUSTOMERS)
                && auth.hasPermission(Permissions.POS);
        System.out.println((adminAll ? "PASS" : "FAIL") + " - admin has admin-only permissions");

        // Create worker with default perms and check nav-relevant permissions.
        com.dns.bookshop.db.repositories.UserRepository ur = new com.dns.bookshop.db.repositories.UserRepository();
        com.dns.bookshop.models.User w = new com.dns.bookshop.models.User("wk",
                com.dns.bookshop.util.PasswordUtil.hash("pass12345"), "Worker", "WORKER");
        ur.create(w);
        ur.savePermissions(w.getId(), UsersPanel.defaultWorkerPerms());

        auth.login("wk", "pass12345");
        boolean wCanSale = auth.hasPermission(Permissions.POS);
        boolean wNoManageUsers = !auth.hasPermission(Permissions.MANAGE_USERS);
        boolean wNoAddCustomers = !auth.hasPermission(Permissions.ADD_CUSTOMERS);
        boolean wNoDeleteProducts = !auth.hasPermission(Permissions.DELETE_PRODUCTS);
        System.out.println((wCanSale ? "PASS" : "FAIL") + " - worker can use POS");
        System.out.println((wNoManageUsers ? "PASS" : "FAIL") + " - worker cannot manage users");
        System.out.println((wNoAddCustomers ? "PASS" : "FAIL") + " - worker cannot add customers");
        System.out.println((wNoDeleteProducts ? "PASS" : "FAIL") + " - worker cannot delete products");

        // Grant a permission then re-login to confirm it takes effect.
        com.dns.bookshop.models.Permissions granted = com.dns.bookshop.ui.panels.UsersPanel.defaultWorkerPerms();
        granted.grant(Permissions.ADD_CUSTOMERS);
        ur.savePermissions(w.getId(), granted);
        auth.login("wk", "pass12345");
        boolean nowCanAddCustomers = auth.hasPermission(Permissions.ADD_CUSTOMERS);
        System.out.println((nowCanAddCustomers ? "PASS" : "FAIL") + " - granting add_customers takes effect");
        System.out.println("ALL AUTH CHECKS DONE");
    }
}
