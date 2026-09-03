package com.dns.bookshop.db.repositories;

import com.dns.bookshop.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple audit trail - logs every significant user action.
 */
public class AuditLogRepository {

    private final Connection conn;

    public AuditLogRepository() {
        this.conn = Database.getInstance().getConnection();
    }

    public void log(Integer userId, String username, String action, String details) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO audit_log (user_id, username, action, details, created_at) VALUES (?,?,?,?,?)")) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER);
            else ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit failure should never crash the app.
            System.err.println("Audit log error: " + e.getMessage());
        }
    }

    public List<String[]> recent(int limit) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                        String.valueOf(rs.getLong("created_at")),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("details")
                    });
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }
}
