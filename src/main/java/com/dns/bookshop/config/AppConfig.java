package com.dns.bookshop.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration and paths.
 * All data is stored locally, in a "data" sub-folder sitting NEXT TO the
 * application itself. This keeps the whole system fully portable: you can copy
 * the installation folder to any PC and take all products, sales and settings
 * with you. No server, nothing installed outside the folder.
 */
public final class AppConfig {

    public static final String APP_NAME = "DNS BookShop";
    public static final String APP_VERSION = "2.0.0";

    // Companies cannot be edited here generally but constants keep shop identity central.
    public static final String SHOP_NAME = "DNS BookShop";
    public static final String SHOP_TAGLINE = "Books & Stationery";

    // Directory where application data (DB, exports, barcodes) is stored.
    // Resolved to the folder that contains the running application (the jar),
    // so the program, its data, exports and barcodes all move together as one
    // unit when copied to the customer's shop PC.
    private static Path dataDir;

    private AppConfig() {}

    /**
     * Returns the folder that contains the running application. When launched
     * from a jar this is the jar's own directory; when launched from classes it
     * falls back to the working directory.
     */
    private static Path appDir() {
        try {
            java.net.URI uri = AppConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path loc = Paths.get(uri);
            if (loc.toFile().isFile()) {
                return loc.toAbsolutePath().getParent();
            }
            return loc.toAbsolutePath();
        } catch (Exception ignored) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        }
    }

    public static Path dataDir() {
        if (dataDir == null) {
            // Portable: <install-folder>/data
            dataDir = appDir().resolve("data");
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
