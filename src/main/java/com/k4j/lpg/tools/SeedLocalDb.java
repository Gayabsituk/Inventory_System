package com.k4j.lpg.tools;

import com.k4j.lpg.models.Product;
import com.k4j.lpg.models.User;
import com.k4j.lpg.services.LocalDbService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Simple CLI tool to seed the local SQLite cache DB used by the app.
 * Run after building the shaded jar:
 * java -cp src/target/k4j-inventory-system-1.0.0-shaded.jar com.k4j.lpg.tools.SeedLocalDb
 */
public class SeedLocalDb {
    public static void main(String[] args) {
        try {
            System.out.println("Initializing local DB (creating tables if needed)...");
            LocalDbService.initialize();

            // Create admin user with password
            LocalDbService.addUser("admin", "admin123", "admin");

            // Seed a few sample products
            List<Product> products = Arrays.asList(
                    new Product(UUID.randomUUID().toString(), "Propane Cylinder 11kg", "Gas", 50, 1200.00, 20),
                    new Product(UUID.randomUUID().toString(), "Regulator", "Accessory", 150, 450.00, 20),
                    new Product(UUID.randomUUID().toString(), "Gas Hose 1m", "Accessory", 80, 150.00, 20)
            );

            LocalDbService.cacheProducts(products);

            System.out.println("Seeding complete: admin user + " + products.size() + " products added.");
            System.out.println("DB file: k4j_cache.db (in working directory)");
        } catch (Exception e) {
            System.err.println("Failed to seed local DB: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
