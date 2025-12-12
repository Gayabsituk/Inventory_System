package com.k4j.lpg.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.k4j.lpg.models.Product;
import com.k4j.lpg.models.User;
import com.k4j.lpg.utils.Config;
import com.k4j.lpg.utils.SessionManager;

import com.k4j.lpg.services.LocalDbService;
import com.k4j.lpg.utils.NetworkChecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    // ============================================
    // AUTH API
    // ============================================
    
    /**
     * Sign in user
     */
    public static ApiResponse<User> signIn(String username, String password) {
        try {
            // Try local authentication first
            User user = LocalDbService.authenticateUser(username, password);
            
            if (user != null) {
                // Generate a simple local token
                String localToken = "local_" + java.util.UUID.randomUUID().toString();
                
                // Save session
                SessionManager.getInstance().saveSession(localToken, user);
                
                logger.info("Local sign in successful for user: " + username);
                return new ApiResponse<>(true, user, null);
            }
            
            // If offline and no local match
            if (!NetworkChecker.isOnline()) {
                return new ApiResponse<>(false, null, "Invalid username or password (Offline mode)");
            }
            
            // Try online authentication if available
            try {
                Map<String, String> body = new HashMap<>();
                body.put("username", username);
                body.put("password", password);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_BASE_URL + "/auth/signin"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + Config.SUPABASE_ANON_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                    
                    if (jsonResponse.get("success").getAsBoolean()) {
                        String accessToken = jsonResponse.get("accessToken").getAsString();
                        JsonObject userJson = jsonResponse.getAsJsonObject("user");
                        
                        User onlineUser = new User(
                            userJson.get("id").getAsString(),
                            userJson.get("username").getAsString(),
                            userJson.get("role").getAsString()
                        );
                        
                        // Cache the user locally
                        LocalDbService.addUser(username, password, onlineUser.getRole());
                        
                        // Save session
                        SessionManager.getInstance().saveSession(accessToken, onlineUser);
                        
                        logger.info("Online sign in successful for user: " + username);
                        return new ApiResponse<>(true, onlineUser, null);
                    }
                }
            } catch (Exception e) {
                logger.warn("Online authentication failed, already tried local auth", e);
            }
            
            return new ApiResponse<>(false, null, "Invalid username or password");
            
        } catch (Exception e) {
            logger.error("Sign in error", e);
            return new ApiResponse<>(false, null, "Sign in failed: " + e.getMessage());
        }
    }
    
    /**
     * Sign up new user
     */
    public static ApiResponse<User> signUp(String username, String password, String role) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);
            body.put("role", role);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_BASE_URL + "/auth/signup"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Config.SUPABASE_ANON_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonObject userJson = jsonResponse.getAsJsonObject("user");
                
                User user = new User(
                    userJson.get("id").getAsString(),
                    userJson.get("username").getAsString(),
                    userJson.get("role").getAsString()
                );
                
                logger.info("Sign up successful for user: " + username);
                return new ApiResponse<>(true, user, null);
            }
            
            JsonObject errorResponse = gson.fromJson(response.body(), JsonObject.class);
            String errorMsg = errorResponse.has("error") ? errorResponse.get("error").getAsString() : "Signup failed";
            
            return new ApiResponse<>(false, null, errorMsg);
            
        } catch (Exception e) {
            logger.error("Sign up error", e);
            return new ApiResponse<>(false, null, "Sign up failed: " + e.getMessage());
        }
    }
    
    /**
     * Check session validity
     */
    public static ApiResponse<User> checkSession() {
        try {
            String token = SessionManager.getInstance().getAccessToken();
            if (token == null) {
                return new ApiResponse<>(false, null, "No session token");
            }
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_BASE_URL + "/auth/session"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonObject userJson = jsonResponse.getAsJsonObject("user");
                
                User user = new User(
                    userJson.get("id").getAsString(),
                    userJson.get("username").getAsString(),
                    userJson.get("role").getAsString()
                );
                
                return new ApiResponse<>(true, user, null);
            }
            
            // Session invalid, clear it
            SessionManager.getInstance().clearSession();
            return new ApiResponse<>(false, null, "Session expired");
            
        } catch (Exception e) {
            logger.error("Session check error", e);
            SessionManager.getInstance().clearSession();
            return new ApiResponse<>(false, null, "Session check failed: " + e.getMessage());
        }
    }
    
    /**
     * Sign out user
     */
    public static void signOut() {
        SessionManager.getInstance().clearSession();
        logger.info("User signed out");
    }
    
    // ============================================
    // PRODUCTS API
    // ============================================
    
    /**
     * Get all products
     */
    
    public static ApiResponse<List<Product>> getProducts() {
    try {
        // Check if online
        if (NetworkChecker.isOnline()) {
            // Fetch from Supabase
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_BASE_URL + "/products"))
                    .header("Authorization", "Bearer " + Config.SUPABASE_ANON_KEY)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                
                List<Product> products = new ArrayList<>();
                jsonResponse.getAsJsonArray("products").forEach(element -> {
                    JsonObject productJson = element.getAsJsonObject();
                    Product product = new Product(
                        productJson.get("id").getAsString(),
                        productJson.get("name").getAsString(),
                        productJson.get("category").getAsString(),
                        productJson.get("quantity").getAsInt(),
                        productJson.get("price").getAsDouble(),
                        productJson.has("low_stock_threshold") ? productJson.get("low_stock_threshold").getAsInt() : Config.LOW_STOCK_THRESHOLD
                    );
                    products.add(product);
                });
                
                // Cache the products
                try {
                    LocalDbService.cacheProducts(products);
                    logger.info("Products cached locally");
                } catch (SQLException e) {
                    logger.error("Failed to cache products", e);
                }
                
                return new ApiResponse<>(true, products, null);
            }
        }
        
        // If offline or failed, use cache
        logger.warn("Using cached products (offline mode)");
        List<Product> cachedProducts = LocalDbService.getCachedProducts();
        return new ApiResponse<>(true, cachedProducts, "Using cached data (offline)");
        
    } catch (Exception e) {
        // Fallback to cache
        try {
            logger.error("Failed to fetch products, using cache", e);
            List<Product> cachedProducts = LocalDbService.getCachedProducts();
            return new ApiResponse<>(true, cachedProducts, "Using cached data (error: " + e.getMessage() + ")");
        } catch (SQLException cacheError) {
            logger.error("Failed to load cached products", cacheError);
            return new ApiResponse<>(false, null, "Failed to load products: " + e.getMessage());
        }
    }
}
    
    /**
     * Add new product (Admin only)
     */
    public static ApiResponse<Product> addProduct(Product product) {
        try {
            Product newProduct = LocalDbService.addProduct(
                product.getName(),
                product.getCategory(),
                product.getQuantity(),
                product.getPrice()
            );
            
            logger.info("Product added: " + newProduct.getName());
            return new ApiResponse<>(true, newProduct, null);
            
        } catch (Exception e) {
            logger.error("Add product error", e);
            return new ApiResponse<>(false, null, "Failed to add product: " + e.getMessage());
        }
    }
    
    /**
     * Update product (Admin only for full updates, Staff for quantity only)
     */
    public static ApiResponse<Product> updateProduct(String productId, Map<String, Object> updates) {
        try {
            // Fetch current product to preserve fields not being updated
            Product currentProduct = LocalDbService.getProductById(productId);
            if (currentProduct == null) {
                return new ApiResponse<>(false, null, "Product not found");
            }
            
            // Use provided values or fall back to current values
            String name = updates.containsKey("name") ? (String) updates.get("name") : currentProduct.getName();
            String category = updates.containsKey("category") ? (String) updates.get("category") : currentProduct.getCategory();
            Integer quantity = updates.containsKey("quantity") ? (Integer) updates.get("quantity") : currentProduct.getQuantity();
            Double price = updates.containsKey("price") ? (Double) updates.get("price") : currentProduct.getPrice();
            Integer lowStockThreshold = updates.containsKey("lowStockThreshold") ? (Integer) updates.get("lowStockThreshold") : currentProduct.getLowStockThreshold();
            
            LocalDbService.updateProduct(productId, name, category, quantity, price, lowStockThreshold);
            
            Product updatedProduct = LocalDbService.getProductById(productId);
            logger.info("Product updated: " + (updatedProduct != null ? updatedProduct.getName() : productId));
            return new ApiResponse<>(true, updatedProduct, null);
            
        } catch (Exception e) {
            logger.error("Update product error", e);
            return new ApiResponse<>(false, null, "Failed to update product: " + e.getMessage());
        }
    }
    
    /**
     * Delete product (Admin only)
     */
    public static ApiResponse<Void> deleteProduct(String productId) {
        try {
            LocalDbService.deleteProduct(productId);
            
            logger.info("Product deleted: " + productId);
            return new ApiResponse<>(true, null, null);
            
        } catch (Exception e) {
            logger.error("Delete product error", e);
            return new ApiResponse<>(false, null, "Failed to delete product: " + e.getMessage());
        }
    }
    
    // ============================================
    // USERS API (Admin only)
    // ============================================
    
    /**
     * Get all users (Admin only)
     */
    public static ApiResponse<List<User>> getUsers() {
        try {
            List<User> users = LocalDbService.getAllUsers();
            
            logger.info("Loaded " + users.size() + " users");
            return new ApiResponse<>(true, users, null);
            
        } catch (Exception e) {
            logger.error("Get users error", e);
            return new ApiResponse<>(false, null, "Failed to load users: " + e.getMessage());
        }
    }
    
    /**
     * Update user (Admin only)
     */
    public static ApiResponse<User> updateUser(String userId, Map<String, Object> updates) {
        try {
            String token = SessionManager.getInstance().getAccessToken();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_BASE_URL + "/users/" + userId))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(updates)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonObject userJson = jsonResponse.getAsJsonObject("user");
                
                User updatedUser = new User(
                    userJson.get("id").getAsString(),
                    userJson.get("username").getAsString(),
                    userJson.get("role").getAsString()
                );
                
                logger.info("User updated: " + updatedUser.getUsername());
                return new ApiResponse<>(true, updatedUser, null);
            }
            
            return new ApiResponse<>(false, null, "Failed to update user");
            
        } catch (Exception e) {
            logger.error("Update user error", e);
            return new ApiResponse<>(false, null, "Failed to update user: " + e.getMessage());
        }
    }
    
    /**
     * Delete user (Admin only)
     */
    public static ApiResponse<Void> deleteUser(String userId) {
        try {
            String token = SessionManager.getInstance().getAccessToken();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_BASE_URL + "/users/" + userId))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logger.info("User deleted: " + userId);
                return new ApiResponse<>(true, null, null);
            }
            
            return new ApiResponse<>(false, null, "Failed to delete user");
            
        } catch (Exception e) {
            logger.error("Delete user error", e);
            return new ApiResponse<>(false, null, "Failed to delete user: " + e.getMessage());
        }
    }
    
    /**
     * Initialize database
     */
    public static ApiResponse<Void> initializeDatabase() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_BASE_URL + "/init"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Config.SUPABASE_ANON_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logger.info("Database initialized successfully");
                return new ApiResponse<>(true, null, null);
            }
            
            return new ApiResponse<>(false, null, "Failed to initialize database");
            
        } catch (Exception e) {
            logger.error("Initialize database error", e);
            return new ApiResponse<>(false, null, "Failed to initialize database: " + e.getMessage());
        }
    }
    
    /**
     * Generic API Response wrapper
     */
    public static class ApiResponse<T> {
        private final boolean success;
        private final T data;
        private final String error;
        
        public ApiResponse(boolean success, T data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public T getData() {
            return data;
        }
        
        public String getError() {
            return error;
        }
    }
}
