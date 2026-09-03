package com.dns.bookshop.services;

import com.dns.bookshop.config.AppConfig;
import com.dns.bookshop.db.Database;
import com.dns.bookshop.db.repositories.AuditLogRepository;
import com.dns.bookshop.db.repositories.ProductRepository;
import com.dns.bookshop.models.Product;
import com.dns.bookshop.models.User;
import com.dns.bookshop.services.AuthService;

import java.util.List;

/**
 * Business logic for product registration, scanning, barcode generation and stock.
 */
public class ProductService {

    private final ProductRepository productRepo;
    private final BarcodeService barcodeService;
    private final AuditLogRepository audit;

    public ProductService() {
        this.productRepo = new ProductRepository();
        this.barcodeService = new BarcodeService();
        this.audit = new AuditLogRepository();
    }

    /**
     * Looks up a product by the scanned barcode.
     */
    public Product findByBarcode(String rawBarcode) {
        String barcode = Database.normaliseBarcode(rawBarcode);
        return productRepo.findByBarcode(barcode);
    }

    /**
     * Registers a product. If barcode is blank/auto, one is generated.
     * Returns the saved product.
     */
    public Product register(Product p, boolean autoGenerateBarcode, boolean generateAsBook) {
        // Validate
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (p.getSellingPrice() < 0 || p.getPurchasePrice() < 0) {
            throw new IllegalArgumentException("Prices cannot be negative.");
        }
        if (p.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }

        boolean hadBarcode = p.getBarcode() != null && !p.getBarcode().trim().isEmpty();

        if (autoGenerateBarcode || !hadBarcode) {
            String barcode = generateAsBook
                    ? barcodeService.generateBookISBN()
                    : barcodeService.generateItemBarcode();
            p.setBarcode(barcode);
            p.setBarcodeGenerated(true);
        } else {
            String bc = p.getBarcode().trim();
            // Ensure uniqueness - an existing product with the same barcode
            // would collide, which is not allowed for new registrations.
            Product existing = productRepo.findByBarcode(bc);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "Barcode '" + bc + "' is already registered to: " + existing.getName());
            }
            p.setBarcode(bc);
        }

        long now = System.currentTimeMillis();
        p.setCreatedAt(now);
        User cu = AuthService.getInstance().getCurrentUser();
        productRepo.create(p);

        // Record initial stock movement for the starting quantity.
        if (p.getStockQuantity() > 0) {
            try (var ps = Database.getInstance().getConnection().prepareStatement(
                    "INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, note, created_at) VALUES (?,?,?,?,?,?)")) {
                ps.setInt(1, p.getId());
                if (cu == null) ps.setNull(2, java.sql.Types.INTEGER);
                else ps.setInt(2, cu.getId());
                ps.setString(3, "IN");
                ps.setInt(4, p.getStockQuantity());
                ps.setString(5, "Initial stock on registration");
                ps.setLong(6, now);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        audit.log(cu == null ? null : cu.getId(), cu == null ? "system" : cu.getUsername(),
                "PRODUCT_ADD", "Added '" + p.getName() + "' barcode=" + p.getBarcode() + " qty=" + p.getStockQuantity());
        return productRepo.findById(p.getId());
    }

    public void update(Product p) {
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        productRepo.update(p);
        User cu = AuthService.getInstance().getCurrentUser();
        audit.log(cu == null ? null : cu.getId(), cu == null ? "system" : cu.getUsername(),
                "PRODUCT_EDIT", "Updated '" + p.getName() + "'");
    }

    public void delete(int productId) {
        Product p = productRepo.findById(productId);
        productRepo.delete(productId);
        User cu = AuthService.getInstance().getCurrentUser();
        audit.log(cu == null ? null : cu.getId(), cu == null ? "system" : cu.getUsername(),
                "PRODUCT_DELETE", "Deleted '" + (p == null ? "#"+productId : p.getName()) + "'");
    }

    /**
     * Adds stock to a product (restocking) and records the movement.
     */
    public void restock(int productId, int qty, User user) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        Product p = productRepo.findById(productId);
        if (p == null) throw new IllegalArgumentException("Product not found.");
        int newQty = p.getStockQuantity() + qty;
        productRepo.updateStockQty(productId, newQty);
        try (var ps = Database.getInstance().getConnection().prepareStatement(
                "INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, note, created_at) VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, productId);
            if (user == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, user.getId());
            ps.setString(3, "IN");
            ps.setInt(4, qty);
            ps.setString(5, "Manual restock");
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        audit.log(user == null ? null : user.getId(), user == null ? "system" : user.getUsername(),
                "STOCK_IN", p.getName() + " +" + qty);
    }

    public void setStock(int productId, int qty, User user) {
        if (qty < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        Product p = productRepo.findById(productId);
        if (p == null) throw new IllegalArgumentException("Product not found.");
        int diff = qty - p.getStockQuantity();
        productRepo.updateStockQty(productId, qty);
        try (var ps = Database.getInstance().getConnection().prepareStatement(
                "INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, note, created_at) VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, productId);
            if (user == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, user.getId());
            ps.setString(3, "ADJUST");
            ps.setInt(4, diff);
            ps.setString(5, "Manual adjustment");
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        audit.log(user == null ? null : user.getId(), user == null ? "system" : user.getUsername(),
                "STOCK_ADJUST", p.getName() + " set to " + qty);
    }

    public List<Product> search(String q) { return productRepo.search(q); }
    public List<Product> findAll() { return productRepo.findAll(); }
    public List<Product> findLowStock() { return productRepo.findLowStock(); }
    public Product findById(int id) { return productRepo.findById(id); }

    public BarcodeService barcodeService() { return barcodeService; }

    public static String formatCategory(String raw) {
        if ("BOOK".equalsIgnoreCase(raw)) return "Book";
        if ("STATIONERY".equalsIgnoreCase(raw)) return "Stationery";
        return raw;
    }
}
