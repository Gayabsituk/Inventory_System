package com.k4j.lpg.controllers;

import com.k4j.lpg.Main;
import com.k4j.lpg.models.Product;
import com.k4j.lpg.models.User;
import com.k4j.lpg.services.ApiService;
import com.k4j.lpg.utils.Config;
import com.k4j.lpg.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Admin Dashboard Controller
 * Equivalent to AdminDashboard.tsx
 */
public class AdminDashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label totalUsersLabel;
    
    @FXML private TextField searchField;
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;
    
    private final ObservableList<Product> productsList = FXCollections.observableArrayList();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;
    
    @FXML
    public void initialize() {
        // Set welcome message
        String username = SessionManager.getInstance().getCurrentUsername();
        welcomeLabel.setText("Welcome, " + username + " (Admin)");
        
        // Setup table columns
        setupProductsTable();
        
        // Setup search
        filteredProducts = new FilteredList<>(productsList, p -> true);
        productsTable.setItems(filteredProducts);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });
        
        // Load data
        loadProducts();
        loadUsers();
    }
    
    private void setupProductsTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        
        // Custom quantity cell factory to highlight low stock
        quantityColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                
                if (empty || quantity == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(quantity.toString());
                    if (quantity <= Config.LOW_STOCK_THRESHOLD) {
                        setStyle("-fx-text-fill: #bf3039; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Price formatting
        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%.2f", price));
                }
            }
        });
        
        // Actions column with Edit and Delete buttons
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            
            {
                editBtn.getStyleClass().add("action-button");
                deleteBtn.getStyleClass().add("danger-button");
                
                editBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });
                
                deleteBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    VBox buttons = new VBox(5, editBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });
        
        // Row factory to highlight low stock
        productsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                
                if (empty || product == null) {
                    setStyle("");
                } else if (product.isLowStock()) {
                    getStyleClass().add("low-stock-row");
                } else {
                    getStyleClass().remove("low-stock-row");
                }
            }
        });
    }
    
    private void loadProducts() {
        Task<ApiService.ApiResponse<java.util.List<Product>>> task = new Task<>() {
            @Override
            protected ApiService.ApiResponse<java.util.List<Product>> call() {
                return ApiService.getProducts();
            }
        };
        
        task.setOnSucceeded(event -> {
            ApiService.ApiResponse<java.util.List<Product>> response = task.getValue();
            
            if (response.isSuccess()) {
                productsList.clear();
                productsList.addAll(response.getData());
                updateStatistics();
                logger.info("Products loaded: " + productsList.size());
            } else {
                showError("Failed to load products", response.getError());
            }
        });
        
        new Thread(task).start();
    }
    
    private void loadUsers() {
        Task<ApiService.ApiResponse<java.util.List<User>>> task = new Task<>() {
            @Override
            protected ApiService.ApiResponse<java.util.List<User>> call() {
                return ApiService.getUsers();
            }
        };
        
        task.setOnSucceeded(event -> {
            ApiService.ApiResponse<java.util.List<User>> response = task.getValue();
            
            if (response.isSuccess()) {
                usersList.clear();
                usersList.addAll(response.getData());
                updateStatistics();
                logger.info("Users loaded: " + usersList.size());
            } else {
                showError("Failed to load users", response.getError());
            }
        });
        
        new Thread(task).start();
    }
    
    private void updateStatistics() {
        Platform.runLater(() -> {
            totalProductsLabel.setText(String.valueOf(productsList.size()));
            
            long lowStockCount = productsList.stream()
                    .filter(Product::isLowStock)
                    .count();
            lowStockLabel.setText(String.valueOf(lowStockCount));
            
            totalUsersLabel.setText(String.valueOf(usersList.size()));
        });
    }
    
    private void filterProducts(String searchText) {
        filteredProducts.setPredicate(product -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            
            String lowerCaseFilter = searchText.toLowerCase();
            
            return product.getName().toLowerCase().contains(lowerCaseFilter) ||
                   product.getCategory().toLowerCase().contains(lowerCaseFilter);
        });
    }
    
    @FXML
    private void handleAddProduct() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add Product");
        dialog.setHeaderText("Add New Product");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Product name");
        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 10000, 0);
        TextField priceField = new TextField();
        priceField.setPromptText("0.00");
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantitySpinner, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    return new Product(
                        null,
                        nameField.getText(),
                        categoryField.getText(),
                        quantitySpinner.getValue(),
                        Double.parseDouble(priceField.getText())
                    );
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Please enter a valid price");
                    return null;
                }
            }
            return null;
        });
        
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(this::addProduct);
    }
    
    private void addProduct(Product product) {
        Task<ApiService.ApiResponse<Product>> task = new Task<>() {
            @Override
            protected ApiService.ApiResponse<Product> call() {
                return ApiService.addProduct(product);
            }
        };
        
        task.setOnSucceeded(event -> {
            ApiService.ApiResponse<Product> response = task.getValue();
            
            if (response.isSuccess()) {
                productsList.add(response.getData());
                updateStatistics();
                showSuccess("Product added successfully");
            } else {
                showError("Failed to add product", response.getError());
            }
        });
        
        new Thread(task).start();
    }
    
    private void handleEditProduct(Product product) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Edit Product");
        dialog.setHeaderText("Edit: " + product.getName());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(product.getName());
        TextField categoryField = new TextField(product.getCategory());
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 10000, product.getQuantity());
        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantitySpinner, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", nameField.getText());
                updates.put("category", categoryField.getText());
                updates.put("quantity", quantitySpinner.getValue());
                try {
                    updates.put("price", Double.parseDouble(priceField.getText()));
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Please enter a valid price");
                    return null;
                }
                return updates;
            }
            return null;
        });
        
        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(updates -> updateProduct(product.getId(), updates));
    }
    
    private void updateProduct(String productId, Map<String, Object> updates) {
        Task<ApiService.ApiResponse<Product>> task = new Task<>() {
            @Override
            protected ApiService.ApiResponse<Product> call() {
                return ApiService.updateProduct(productId, updates);
            }
        };
        
        task.setOnSucceeded(event -> {
            ApiService.ApiResponse<Product> response = task.getValue();
            
            if (response.isSuccess()) {
                loadProducts(); // Reload to refresh table
                showSuccess("Product updated successfully");
            } else {
                showError("Failed to update product", response.getError());
            }
        });
        
        new Thread(task).start();
    }
    
    private void handleDeleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete: " + product.getName());
        alert.setContentText("Are you sure you want to delete this product?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteProduct(product);
        }
    }
    
    private void deleteProduct(Product product) {
        Task<ApiService.ApiResponse<Void>> task = new Task<>() {
            @Override
            protected ApiService.ApiResponse<Void> call() {
                return ApiService.deleteProduct(product.getId());
            }
        };
        
        task.setOnSucceeded(event -> {
            ApiService.ApiResponse<Void> response = task.getValue();
            
            if (response.isSuccess()) {
                productsList.remove(product);
                updateStatistics();
                showSuccess("Product deleted successfully");
            } else {
                showError("Failed to delete product", response.getError());
            }
        });
        
        new Thread(task).start();
    }
    
    @FXML
    private void handleManageUsers() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Manage Users");
        alert.setHeaderText("User Management");
        alert.setContentText("Total Users: " + usersList.size() + "\n\n" +
                "User management UI coming soon!");
        alert.showAndWait();
    }
    
    @FXML
    private void handleRefresh() {
        loadProducts();
        loadUsers();
    }
    
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Confirm Logout");
        alert.setContentText("Are you sure you want to logout?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Main.logout();
        }
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
