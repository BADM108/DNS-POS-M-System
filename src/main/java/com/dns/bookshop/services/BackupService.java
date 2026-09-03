package com.dns.bookshop.services;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.db.Database;
import com.dns.bookshop.db.repositories.SaleRepository;
import com.dns.bookshop.models.Sale;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handles local backup/export of data (all stored on the same PC).
 */
public class BackupService {

    private final SaleRepository saleRepo;

    public BackupService() {
        this.saleRepo = new SaleRepository();
    }

    /**
     * Creates a timestamped copy of the SQLite database file inside exports dir.
     */
    public File exportDatabaseBackup() throws IOException {
        File src = new File(AppConfig.databasePath());
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File dest = AppConfig.exportsDir().resolve("backup-" + stamp + ".db").toFile();
        if (!src.exists()) throw new IOException("Database file does not exist yet.");
        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    /**
     * Exports sales history (including items) to a CSV file.
     */
    public File exportSalesCsv(long start, long end) throws IOException {
        List<Sale> sales = saleRepo.findBetween(start, end);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File file = AppConfig.exportsDir().resolve("sales-export-" + stamp + ".csv").toFile();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write("Invoice,Date,Cashier,Customer,Item,Quantity,UnitPrice,LineTotal,Subtotal,Discount,Tax,Total,PaymentMethod,Status");
            w.newLine();
            for (Sale s : sales) {
                String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .format(java.time.Instant.ofEpochMilli(s.getCreatedAt()));
                String cashier = cashierName(s.getUserId());
                String customer = customerName(s.getCustomerId());
                if (s.getItems().isEmpty()) {
                    w.write(csv(s.getInvoiceNumber()) + "," + csv(date) + "," + csv(cashier) + "," + csv(customer)
                            + ",,,,,," + s.getSubtotal() + "," + s.getDiscount() + "," + s.getTax() + "," + s.getTotal()
                            + "," + csv(s.getPaymentMethod()) + "," + csv(s.getStatus()));
                    w.newLine();
                } else {
                    for (var it : s.getItems()) {
                        w.write(csv(s.getInvoiceNumber()) + "," + csv(date) + "," + csv(cashier) + "," + csv(customer)
                                + "," + csv(it.getProductName()) + "," + it.getQuantity() + "," + it.getUnitPrice()
                                + "," + it.getLineTotal() + "," + s.getSubtotal() + "," + s.getDiscount() + ","
                                + s.getTax() + "," + s.getTotal() + "," + csv(s.getPaymentMethod()) + "," + csv(s.getStatus()));
                        w.newLine();
                    }
                }
            }
        }
        return file;
    }

    private String cashierName(int userId) {
        var u = new com.dns.bookshop.db.repositories.UserRepository().findById(userId);
        return u == null ? "#" + userId : u.getFullName();
    }

    private String customerName(Integer customerId) {
        if (customerId == null) return "Walk-in";
        var c = new com.dns.bookshop.db.repositories.CustomerRepository().findById(customerId);
        return c == null ? "#" + customerId : c.getName();
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
