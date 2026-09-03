package com.dns.bookshop.db.repositories;

import com.dns.bookshop.db.Database;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for users and their permissions.
 */
public class UserRepository {

    private final Connection conn;

    public UserRepository() {
        this.conn = Database.getInstance().getConnection();
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY role, full_name";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<User> findWorkers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role <> 'ADMIN' ORDER BY full_name";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public int create(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, active, created_at) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            ps.setInt(5, user.isActive() ? 1 : 0);
            ps.setLong(6, user.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                    return user.getId();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public void update(User user) {
        String sql = "UPDATE users SET full_name=?, role=?, active=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getRole());
            ps.setInt(3, user.isActive() ? 1 : 0);
            ps.setInt(4, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changePassword(int userId, String newHash) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int userId) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---- Permissions ----

    public Permissions getPermissions(int userId) {
        Permissions perms = new Permissions();
        String sql = "SELECT perm_key, granted FROM user_permissions WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    perms.set(rs.getString(1), rs.getInt(2) == 1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return perms;
    }

    /**
     * Replaces all permissions for a user with the given set.
     * Admin users are not stored in this table (they implicitly have all).
     */
    public void savePermissions(int userId, Permissions perms) {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM user_permissions WHERE user_id=?")) {
            del.setInt(1, userId);
            del.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String sql = "INSERT INTO user_permissions (user_id, perm_key, granted) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String key : Permissions.allKeys()) {
                ps.setInt(1, userId);
                ps.setString(2, key);
                ps.setInt(3, perms.has(key) ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getInt("active") == 1);
        u.setCreatedAt(rs.getLong("created_at"));
        return u;
    }
}
