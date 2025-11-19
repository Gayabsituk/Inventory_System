package com.k4j.lpg.services;

import com.k4j.lpg.models.Product;
import com.k4j.lpg.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalDbService {
    
    private static final String DB_URL = "jdbc:sqlite:k4j_cache.db";
    
    public static void initialize() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Create products table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    price REAL NOT NULL,
                    last_updated INTEGER DEFAULT (strftime('%s','now'))
                )
            """);
            
            // Create users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL,
                    created_at INTEGER DEFAULT (strftime('%s','now'))
                )
            """);
            
            // Insert default admin user if not exists
            stmt.execute("""
                INSERT OR IGNORE INTO users (id, username, password, role)
                VALUES ('admin', 'admin', 'admin123', 'admin')
            """);
            
            // Insert default staff user if not exists
            stmt.execute("""
                INSERT OR IGNORE INTO users (id, username, password, role)
                VALUES ('staff1', 'staff', 'staff123', 'staff')
            """);
            
            // Create sync metadata
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT,
                    last_sync INTEGER DEFAULT (strftime('%s','now'))
                )
            """);
        }
    }
    
    // Cache products
    public static void cacheProducts(List<Product> products) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Clear old data
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM products");
            }
            
            // Insert new data
            String sql = "INSERT INTO products (id, name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Product p : products) {
                    pstmt.setString(1, p.getId());
                    pstmt.setString(2, p.getName());
                    pstmt.setString(3, p.getCategory());
                    pstmt.setInt(4, p.getQuantity());
                    pstmt.setDouble(5, p.getPrice());
                    pstmt.executeUpdate();
                }
            }
            
            // Update sync metadata
            updateLastSync("products");
        }
    }
    
    // Get cached products
    public static List<Product> getCachedProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products ORDER BY name")) {
            
            while (rs.next()) {
                Product p = new Product(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    rs.getDouble("price")
                );
                products.add(p);
            }
        }
        
        return products;
    }
    
    // Authenticate user
    public static User authenticateUser(String username, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM users WHERE username = ? AND password = ?")) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new User(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("role")
                );
            }
            return null;
        }
    }
    
    // Add new user
    public static void addUser(String username, String password, String role) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)")) {
            
            String userId = java.util.UUID.randomUUID().toString();
            pstmt.setString(1, userId);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
        }
    }
    
    // Get all users
    public static List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY username")) {
            
            while (rs.next()) {
                User user = new User(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("role")
                );
                users.add(user);
            }
        }
        
        return users;
    }
    
    // Add new product
    public static Product addProduct(String name, String category, int quantity, double price) throws SQLException {
        String productId = java.util.UUID.randomUUID().toString();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO products (id, name, category, quantity, price) VALUES (?, ?, ?, ?, ?)")) {
            
            pstmt.setString(1, productId);
            pstmt.setString(2, name);
            pstmt.setString(3, category);
            pstmt.setInt(4, quantity);
            pstmt.setDouble(5, price);
            pstmt.executeUpdate();
        }
        
        return new Product(productId, name, category, quantity, price);
    }
    
    // Update product
    public static void updateProduct(String productId, String name, String category, int quantity, double price) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE products SET name = ?, category = ?, quantity = ?, price = ? WHERE id = ?")) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setInt(3, quantity);
            pstmt.setDouble(4, price);
            pstmt.setString(5, productId);
            pstmt.executeUpdate();
        }
    }
    
    // Delete product
    public static void deleteProduct(String productId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
            
            pstmt.setString(1, productId);
            pstmt.executeUpdate();
        }
    }
    
    // Get product by ID
    public static Product getProductById(String productId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM products WHERE id = ?")) {
            
            pstmt.setString(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Product(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    rs.getDouble("price")
                );
            }
        }
        
        return null;
    }
    
    // Update user password
    public static void updateUserPassword(String userId, String newPassword) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
            
            pstmt.setString(1, newPassword);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        }
    }
    
    // Update user role
    public static void updateUserRole(String userId, String newRole) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET role = ? WHERE id = ?")) {
            
            pstmt.setString(1, newRole);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        }
    }
    
    // Delete user
    public static void deleteUser(String userId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    // Get last sync time
    public static long getLastSync(String key) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT last_sync FROM sync_metadata WHERE key = ?")) {
            
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("last_sync");
            }
        }
        return 0;
    }
    
    private static void updateLastSync(String key) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO sync_metadata (key, value, last_sync) VALUES (?, ?, strftime('%s','now'))")) {
            
            pstmt.setString(1, key);
            pstmt.setString(2, "synced");
            pstmt.executeUpdate();
        }
    }
}
