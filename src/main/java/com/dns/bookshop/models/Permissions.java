package com.dns.bookshop.models;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the permissions granted to a worker user.
 * The admin always has all permissions. Workers have a subset.
 *
 * Permission keys are documented in the PermissionRegistry.
 */
public class Permissions {

    // Permission keys
    public static final String POS = "pos";                                  // Operate the cash register / POS
    public static final String ADD_PRODUCTS = "add_products";                // Register new products
    public static final String EDIT_PRODUCTS = "edit_products";              // Edit product details
    public static final String DELETE_PRODUCTS = "delete_products";          // Delete products
    public static final String REGISTER_SCANNED = "register_scanned";        // Register items by scanning barcode
    public static final String GENERATE_BARCODES = "generate_barcodes";      // Generate & print barcodes
    public static final String VIEW_CUSTOMERS = "view_customers";            // View customer list
    public static final String ADD_CUSTOMERS = "add_customers";              // Add customers (admin only)
    public static final String EDIT_CUSTOMERS = "edit_customers";
    public static final String DELETE_CUSTOMERS = "delete_customers";
    public static final String VIEW_SALES = "view_sales";                    // View sales history
    public static final String REFUND_SALES = "refund_sales";                // Refund a sale
    public static final String VIEW_REPORTS = "view_reports";                // View reports/dashboard
    public static final String MANAGE_STOCK = "manage_stock";                // Adjust stock levels
    public static final String MANAGE_USERS = "manage_users";                // Manage workers (admin only)
    public static final String BACKUP_DATA = "backup_data";                  // Backup/export data

    private final Map<String, Boolean> grants = new LinkedHashMap<>();

    public Permissions() {
        for (String key : allKeys()) {
            grants.put(key, false);
        }
    }

    public static String[] allKeys() {
        return new String[] {
            POS, ADD_PRODUCTS, EDIT_PRODUCTS, DELETE_PRODUCTS,
            REGISTER_SCANNED, GENERATE_BARCODES,
            VIEW_CUSTOMERS, ADD_CUSTOMERS, EDIT_CUSTOMERS, DELETE_CUSTOMERS,
            VIEW_SALES, REFUND_SALES, VIEW_REPORTS, MANAGE_STOCK,
            MANAGE_USERS, BACKUP_DATA
        };
    }

    public boolean has(String key) {
        Boolean val = grants.get(key);
        return val != null && val;
    }

    public void grant(String key) {
        grants.put(key, true);
    }

    public void revoke(String key) {
        grants.put(key, false);
    }

    public void set(String key, boolean value) {
        grants.put(key, value);
    }

    public Map<String, Boolean> all() {
        return grants;
    }

    public void clear() {
        for (String key : allKeys()) {
            grants.put(key, false);
        }
    }
}
