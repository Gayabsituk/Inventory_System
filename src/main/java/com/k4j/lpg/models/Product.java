package com.k4j.lpg.models;

import javafx.beans.property.*;

/**
 * Product model with JavaFX properties for TableView binding
 * Equivalent to Product type in App.tsx
 */
public class Product {
    private final StringProperty id;
    private final StringProperty name;
    private final StringProperty category;
    private final IntegerProperty quantity;
    private final DoubleProperty price;
    
    public Product() {
        this("", "", "", 0, 0.0);
    }
    
    public Product(String id, String name, String category, int quantity, double price) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.price = new SimpleDoubleProperty(price);
    }
    
    // ID Property
    public String getId() {
        return id.get();
    }
    
    public void setId(String id) {
        this.id.set(id);
    }
    
    public StringProperty idProperty() {
        return id;
    }
    
    // Name Property
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    // Category Property
    public String getCategory() {
        return category.get();
    }
    
    public void setCategory(String category) {
        this.category.set(category);
    }
    
    public StringProperty categoryProperty() {
        return category;
    }
    
    // Quantity Property
    public int getQuantity() {
        return quantity.get();
    }
    
    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }
    
    public IntegerProperty quantityProperty() {
        return quantity;
    }
    
    // Price Property
    public double getPrice() {
        return price.get();
    }
    
    public void setPrice(double price) {
        this.price.set(price);
    }
    
    public DoubleProperty priceProperty() {
        return price;
    }
    
    // Helper method to check if product is low stock (<=20)
    public boolean isLowStock() {
        return quantity.get() <= 20;
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id='" + id.get() + '\'' +
                ", name='" + name.get() + '\'' +
                ", category='" + category.get() + '\'' +
                ", quantity=" + quantity.get() +
                ", price=" + price.get() +
                '}';
    }
}
