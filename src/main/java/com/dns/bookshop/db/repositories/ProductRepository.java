package com.dns.bookshop.db.repositories;

import com.dns.bookshop.db.Database;
import com.dns.bookshop.models.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for products.
 */
public class ProductRepository {

    private final Connection conn;

    public ProductRepository() {
        this.conn = Database.getInstance().getConnection();
    }

    public Product findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public Product findByBarcode(String barcode) {
        if (barcode == null) return null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE barcode=?")) {
            ps.setString(1, barcode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Product> search(String query) {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT * FROM products
            WHERE name LIKE ? OR barcode LIKE ? OR category LIKE ?
            ORDER BY name
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (query == null ? "" : query.trim()) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM products ORDER BY name")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Product> findLowStock() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE stock_quantity <= low_stock_threshold ORDER BY name";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public int create(Product p) {
        String sql = """
            INSERT INTO products (name, category, barcode, supplier, purchase_price,
                                  selling_price, stock_quantity, low_stock_threshold,
                                  barcode_generated, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getCategory());
            ps.setString(3, p.getBarcode());
            ps.setString(4, p.getSupplier());
            ps.setDouble(5, p.getPurchasePrice());
            ps.setDouble(6, p.getSellingPrice());
            ps.setInt(7, p.getStockQuantity());
            ps.setInt(8, p.getLowStockThreshold());
            ps.setInt(9, p.isBarcodeGenerated() ? 1 : 0);
            ps.setLong(10, p.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getInt(1));
                    return p.getId();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public void update(Product p) {
        String sql = """
            UPDATE products SET name=?, category=?, barcode=?, supplier=?,
                   purchase_price=?, selling_price=?, stock_quantity=?,
                   low_stock_threshold=?, barcode_generated=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getCategory());
            ps.setString(3, p.getBarcode());
            ps.setString(4, p.getSupplier());
            ps.setDouble(5, p.getPurchasePrice());
            ps.setDouble(6, p.getSellingPrice());
            ps.setInt(7, p.getStockQuantity());
            ps.setInt(8, p.getLowStockThreshold());
            ps.setInt(9, p.isBarcodeGenerated() ? 1 : 0);
            ps.setInt(10, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateStockQty(int productId, int newQty) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET stock_quantity=? WHERE id=?")) {
            ps.setInt(1, newQty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
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
