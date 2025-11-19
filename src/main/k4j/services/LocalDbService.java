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
                    username TEXT NOT NULL,
                    role TEXT NOT NULL
                )
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
    
    // Cache user
    public static void cacheUser(User user) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO users (id, username, role) VALUES (?, ?, ?)")) {
            
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getRole());
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