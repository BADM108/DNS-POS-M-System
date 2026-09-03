package com.dns.bookshop.db;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the local SQLite database connection and schema.
 * The database is a single file stored inside the application data folder
 * on the PC - no server, everything stays local.
 */
public final class Database {

    private static Database instance;
    private Connection connection;

    private Database() {}

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + AppConfig.databasePath());
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to open local database at " + AppConfig.databasePath(), e);
            }
        }
        return connection;
    }

    /**
     * Creates all tables if they don't exist and seeds default data (admin).
     */
    public void init() {
        Connection conn = getConnection();
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL
                );
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_permissions (
                    user_id INTEGER NOT NULL,
                    perm_key TEXT NOT NULL,
                    granted INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (user_id, perm_key),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    barcode TEXT UNIQUE,
                    supplier TEXT,
                    purchase_price REAL NOT NULL DEFAULT 0,
                    selling_price REAL NOT NULL DEFAULT 0,
                    stock_quantity INTEGER NOT NULL DEFAULT 0,
                    low_stock_threshold INTEGER NOT NULL DEFAULT 5,
                    barcode_generated INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                );
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS customers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    address TEXT,
                    created_at INTEGER NOT NULL
                );
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_number TEXT NOT NULL UNIQUE,
                    user_id INTEGER NOT NULL,
                    customer_id INTEGER,
                    subtotal REAL NOT NULL DEFAULT 0,
                    discount REAL NOT NULL DEFAULT 0,
                    tax REAL NOT NULL DEFAULT 0,
                    total REAL NOT NULL DEFAULT 0,
                    amount_paid REAL NOT NULL DEFAULT 0,
                    change_given REAL NOT NULL DEFAULT 0,
                    payment_method TEXT NOT NULL DEFAULT 'CASH',
                    status TEXT NOT NULL DEFAULT 'COMPLETE',
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (customer_id) REFERENCES customers(id)
                );
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sale_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sale_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    barcode TEXT,
                    quantity INTEGER NOT NULL,
                    unit_price REAL NOT NULL,
                    line_total REAL NOT NULL,
                    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                );
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS stock_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    user_id INTEGER,
                    movement_type TEXT NOT NULL,  -- IN, OUT, ADJUST, SALE
                    quantity INTEGER NOT NULL,
                    note TEXT,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY (product_id) REFERENCES products(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT
                );
                """);

            // Records every significant action for audit.
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    username TEXT,
                    action TEXT NOT NULL,
                    details TEXT,
                    created_at INTEGER NOT NULL
                );
                """);

            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_product_barcode ON products(barcode);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sale_created ON sales(created_at);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_saleitem_sale ON sale_items(sale_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_product_category ON products(category);");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise database schema", e);
        }

        seedDefaults(conn);
    }

    private void seedDefaults(Connection conn) {
        int userCount;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            userCount = rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (userCount == 0) {
            // Create default admin account.
            String defaultPass = "admin";
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, full_name, role, active, created_at) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, "admin");
                ps.setString(2, PasswordUtil.hash(defaultPass));
                ps.setString(3, "System Administrator");
                ps.setString(4, "ADMIN");
                ps.setInt(5, 1);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        // Seed defaults for settings as needed.
        setSettingIfMissing("shop.name", com.dns.bookshop.config.AppConfig.SHOP_NAME);
        setSettingIfMissing("tax.rate", "0");
        setSettingIfMissing("invoice.prefix", "DNS");
    }

    /**
     * Sets a setting only if it does not already exist - used for seeding defaults.
     */
    public void setSettingIfMissing(String key, String value) {
        try (PreparedStatement check = getConnection().prepareStatement("SELECT 1 FROM settings WHERE key=?");
             PreparedStatement insert = getConnection().prepareStatement("INSERT OR IGNORE INTO settings (key, value) VALUES (?,?)")) {
            insert.setString(1, key);
            insert.setString(2, value);
            insert.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSetting(String key, String value) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO settings (key, value) VALUES (?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSetting(String key, String defaultValue) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT value FROM settings WHERE key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return defaultValue;
    }

    /**
     * Convenience: returns a product (or null) directly by barcode.
     * Used by services to check for barcode collisions.
     */
    public com.dns.bookshop.models.Product getProductByBarcode(String barcode) {
        if (barcode == null) return null;
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM products WHERE barcode=?")) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    com.dns.bookshop.models.Product p = new com.dns.bookshop.models.Product();
                    p.setId(rs.getInt("id"));
                    p.setName(rs.getString("name"));
                    p.setCategory(rs.getString("category"));
                    p.setBarcode(rs.getString("barcode"));
                    p.setSupplier(rs.getString("supplier"));
                    p.setPurchasePrice(rs.getDouble("purchase_price"));
                    p.setSellingPrice(rs.getDouble("selling_price"));
                    p.setStockQuantity(rs.getInt("stock_quantity"));
                    p.setLowStockThreshold(rs.getInt("low_stock_threshold"));
                    p.setBarcodeGenerated(rs.getInt("barcode_generated") == 1);
                    p.setCreatedAt(rs.getLong("created_at"));
                    return p;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Normalises a barcode string: trims whitespace and strips a trailing
     * carriage-return/newline that some USB scanners append with Enter.
     */
    public static String normaliseBarcode(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // Some scanners append CR/LF - strip them.
        s = s.replaceAll("\\r\\n|\\r|\\n", "");
        // If a checksum-only overlong numeric code arrives, keep as-is.
        return s;
    }
}
