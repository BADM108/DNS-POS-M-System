package com.dns.bookshop.db.repositories;

import com.dns.bookshop.db.Database;
import com.dns.bookshop.models.Sale;
import com.dns.bookshop.models.SaleItem;
import com.dns.bookshop.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaleRepository {

    private final Connection conn;

    public SaleRepository() {
        this.conn = Database.getInstance().getConnection();
    }

    /**
     * Creates a sale and its items, and decrements product stock, all atomically.
     * Returns the generated sale id.
     */
    public int createSaleWithItems(Sale sale, List<SaleItem> items) {
        try {
            conn.setAutoCommit(false);
            int saleId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO sales (invoice_number, user_id, customer_id, subtotal, discount, tax, total, amount_paid, change_given, payment_method, status, created_at) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sale.getInvoiceNumber());
                ps.setInt(2, sale.getUserId());
                setNullableInt(ps, 3, sale.getCustomerId());
                ps.setDouble(4, sale.getSubtotal());
                ps.setDouble(5, sale.getDiscount());
                ps.setDouble(6, sale.getTax());
                ps.setDouble(7, sale.getTotal());
                ps.setDouble(8, sale.getAmountPaid());
                ps.setDouble(9, sale.getChangeGiven());
                ps.setString(10, sale.getPaymentMethod());
                ps.setString(11, sale.getStatus());
                ps.setLong(12, sale.getCreatedAt());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    saleId = keys.getInt(1);
                    sale.setId(saleId);
                }
            }

            for (SaleItem it : items) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sale_items (sale_id, product_id, product_name, barcode, quantity, unit_price, line_total) "
                                + "VALUES (?,?,?,?,?,?,?)")) {
                    ps.setInt(1, saleId);
                    ps.setInt(2, it.getProductId());
                    ps.setString(3, it.getProductName());
                    ps.setString(4, it.getBarcode());
                    ps.setInt(5, it.getQuantity());
                    ps.setDouble(6, it.getUnitPrice());
                    ps.setDouble(7, it.getLineTotal());
                    ps.executeUpdate();
                }
            }

            // Decrement stock for each item and log the movement.
            String decSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id=? AND stock_quantity>=?";
            String moveSql = "INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, note, created_at) VALUES (?,?,?,?,?,?)";
            for (SaleItem it : items) {
                try (PreparedStatement ps = conn.prepareStatement(decSql)) {
                    ps.setInt(1, it.getQuantity());
                    ps.setInt(2, it.getProductId());
                    ps.setInt(3, it.getQuantity());
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("Insufficient stock for product #" + it.getProductId());
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(moveSql)) {
                    ps.setInt(1, it.getProductId());
                    ps.setInt(2, sale.getUserId());
                    ps.setString(3, "SALE");
                    ps.setInt(4, it.getQuantity());
                    ps.setString(5, "Sale " + sale.getInvoiceNumber());
                    ps.setLong(6, sale.getCreatedAt());
                    ps.executeUpdate();
                }
            }

            conn.commit();
            sale.setId(saleId);
            return saleId;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new RuntimeException("Sale could not be completed: " + e.getMessage(), e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Refunds (voids) a sale and returns stock to inventory.
     */
    public void refundSale(int saleId, int userId) {
        try {
            conn.setAutoCommit(false);
            Sale sale = findById(saleId);
            if (sale == null) throw new RuntimeException("Sale not found");
            List<SaleItem> items = findItems(saleId);
            long now = System.currentTimeMillis();

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE sales SET status=?, user_id=? WHERE id=?")) {
                ps.setString(1, Sale.STATUS_REFUNDED);
                ps.setInt(2, userId);
                ps.setInt(3, saleId);
                ps.executeUpdate();
            }

            String incSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id=?";
            String moveSql = "INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, note, created_at) VALUES (?,?,?,?,?,?)";
            for (SaleItem it : items) {
                try (PreparedStatement ps = conn.prepareStatement(incSql)) {
                    ps.setInt(1, it.getQuantity());
                    ps.setInt(2, it.getProductId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(moveSql)) {
                    ps.setInt(1, it.getProductId());
                    ps.setInt(2, userId);
                    ps.setString(3, "IN");
                    ps.setInt(4, it.getQuantity());
                    ps.setString(5, "Refund of " + sale.getInvoiceNumber());
                    ps.setLong(6, now);
                    ps.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public Sale findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sales WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale s = map(rs);
                    s.setItems(findItems(id));
                    return s;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public Sale findByInvoice(String invoice) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sales WHERE invoice_number=?")) {
            ps.setString(1, invoice);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale s = map(rs);
                    s.setItems(findItems(s.getId()));
                    return s;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Sale> findRecent(int limit) {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Sale> findBetween(long start, long end) {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, start);
            ps.setLong(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<SaleItem> findItems(int saleId) {
        List<SaleItem> list = new ArrayList<>();
        String sql = "SELECT * FROM sale_items WHERE sale_id=? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem si = new SaleItem();
                    si.setId(rs.getInt("id"));
                    si.setSaleId(rs.getInt("sale_id"));
                    si.setProductId(rs.getInt("product_id"));
                    si.setProductName(rs.getString("product_name"));
                    si.setBarcode(rs.getString("barcode"));
                    si.setQuantity(rs.getInt("quantity"));
                    si.setUnitPrice(rs.getDouble("unit_price"));
                    si.setLineTotal(rs.getDouble("line_total"));
                    list.add(si);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /**
     * Aggregate report of sales grouped by day for a date range.
     * Returns map of dayStartTimestamp -> [count, revenue].
     */
    public Map<Long, double[]> dailyRevenue(long start, long end) {
        Map<Long, double[]> out = new HashMap<>();
        String sql = "SELECT created_at, COUNT(*) as cnt, SUM(total) as rev FROM sales "
                + "WHERE created_at BETWEEN ? AND ? GROUP BY (created_at / 86400000) ORDER BY created_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, start);
            ps.setLong(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long dayMs = (rs.getLong("created_at") / 86400000L) * 86400000L;
                    out.put(dayMs, new double[]{rs.getInt("cnt"), rs.getDouble("rev")});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public Map<Integer, Integer> topProducts(long start, long end, int limit) {
        Map<Integer, Integer> out = new java.util.LinkedHashMap<>();
        String sql = """
            SELECT si.product_id, SUM(si.quantity) AS qty
            FROM sale_items si
            JOIN sales s ON s.id = si.sale_id
            WHERE s.created_at BETWEEN ? AND ? AND s.status <> 'REFUNDED'
            GROUP BY si.product_id
            ORDER BY qty DESC LIMIT ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, start);
            ps.setLong(2, end);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getInt(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public double revenueBetweenExcludingRefund(long start, long end) {
        String sql = "SELECT COALESCE(SUM(total),0) FROM sales "
                + "WHERE created_at BETWEEN ? AND ? AND status <> 'REFUNDED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, start);
            ps.setLong(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val == null) ps.setNull(idx, java.sql.Types.INTEGER);
        else ps.setInt(idx, val);
    }

    private Sale map(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setUserId(rs.getInt("user_id"));
        int cid = rs.getInt("customer_id");
        s.setCustomerId(rs.wasNull() ? null : cid);
        s.setSubtotal(rs.getDouble("subtotal"));
        s.setDiscount(rs.getDouble("discount"));
        s.setTax(rs.getDouble("tax"));
        s.setTotal(rs.getDouble("total"));
        s.setAmountPaid(rs.getDouble("amount_paid"));
        s.setChangeGiven(rs.getDouble("change_given"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setStatus(rs.getString("status"));
        s.setCreatedAt(rs.getLong("created_at"));
        return s;
    }
}
