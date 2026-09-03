package com.dns.bookshop.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration and paths.
 * All data is stored locally (inside the application directory), no server.
 */
public final class AppConfig {

    public static final String APP_NAME = "DNS BookShop";
    public static final String APP_VERSION = "1.0.0";

    // Companies cannot be edited here generally but constants keep shop identity central.
    public static final String SHOP_NAME = "DNS BookShop";
    public static final String SHOP_TAGLINE = "Books & Stationery";

    // Directory where application data (DB, exports, barcodes) is stored.
    // Located next to the running application so everything stays on the PC.
    private static Path dataDir;

    private AppConfig() {}

    public static Path dataDir() {
        if (dataDir == null) {
            String userHome = System.getProperty("user.home");
            // Store in user's home under DNSBookShop - reliable across installs
            dataDir = Paths.get(userHome, "DNSBookShop", "data");
            File dir = dataDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return dataDir;
    }

    public static String databasePath() {
        return dataDir().resolve("dns_bookshop.db").toString();
    }

    public static Path exportsDir() {
        Path p = dataDir().resolve("exports");
        File f = p.toFile();
        if (!f.exists()) f.mkdirs();
        return p;
    }

    public static Path barcodesDir() {
        Path p = dataDir().resolve("barcodes");
        File f = p.toFile();
        if (!f.exists()) f.mkdirs();
        return p;
    }

    public static Path logsDir() {
        Path p = dataDir().resolve("logs");
        File f = p.toFile();
        if (!f.exists()) f.mkdirs();
        return p;
    }

    public static void setDataDir(Path dir) {
        dataDir = dir;
    }
}
