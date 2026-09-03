package com.dns.bookshop.services;

import com.dns.bookshop.db.repositories.UserRepository;
import com.dns.bookshop.models.Permissions;
import com.dns.bookshop.models.User;
import com.dns.bookshop.util.PasswordUtil;

/**
 * Handles authentication and sessions for the currently logged-in user.
 */
public class AuthService {

    private static AuthService instance;
    private final UserRepository userRepo;
    private User currentUser;
    private Permissions currentPermissions;

    private AuthService() {
        this.userRepo = new UserRepository();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Authenticates a user. Returns the user or throws on failure.
     */
    public User login(String username, String password) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("This account has been deactivated.");
        }
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        this.currentUser = user;
        loadPermissions(user);
        return user;
    }

    private void loadPermissions(User user) {
        if (user.isAdmin()) {
            Permissions all = new Permissions();
            for (String key : Permissions.allKeys()) {
                all.grant(key);
            }
            currentPermissions = all;
        } else {
            currentPermissions = userRepo.getPermissions(user.getId());
        }
    }

    public void logout() {
        currentUser = null;
        currentPermissions = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean hasPermission(String key) {
        if (currentUser == null) return false;
        if (currentUser.isAdmin()) return true;
        return currentPermissions != null && currentPermissions.has(key);
    }

    public Permissions getPermissions() {
        return currentPermissions;
    }
}
