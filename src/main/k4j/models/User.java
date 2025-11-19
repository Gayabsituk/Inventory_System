package com.k4j.lpg.models;

/**
 * User model
 * Equivalent to User type in App.tsx
 */
public class User {
    private String id;
    private String username;
    private String role; // "admin" or "staff"
    
    public User() {
    }
    
    public User(String id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public boolean isAdmin() {
        return "admin".equals(role);
    }
    
    public boolean isStaff() {
        return "staff".equals(role);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
