package com.k4j.lpg.controllers;

import com.k4j.lpg.Main;
import com.k4j.lpg.models.Product;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Staff Dashboard Controller
 * Equivalent to StaffDashboard.tsx
 */
public class StaffDashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(StaffDashboardController.class);
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    
    @FXML private TextField searchField;
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;
    
    private final ObservableList<Product> productsList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;
    
    @FXML
    public void initialize() {
        // Set welcome message
        String username = SessionManager.getInstance().getCurrentUsername();
        welcomeLabel.setText("Welcome, " + username + " (Staff)");
        
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
        
        // Actions column with Update Quantity button only (Staff can only update quantities)
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button updateBtn = new Button("Update Qty");
            
            {
                updateBtn.getStyleClass().add("action-button");
                
                updateBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleUpdateQuantity(product);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(updateBtn);
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
    
    private void updateStatistics() {
        Platform.runLater(() -> {
            totalProductsLabel.setText(String.valueOf(productsList.size()));
            
            long lowStockCount = productsList.stream()
                    .filter(Product::isLowStock)
                    .count();
            lowStockLabel.setText(String.valueOf(lowStockCount));
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
    
    private void handleUpdateQuantity(Product product) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Update Quantity");
        dialog.setHeaderText("Update quantity for: " + product.getName());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        Label currentLabel = new Label("Current: " + product.getQuantity());
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 10000, product.getQuantity());
        quantitySpinner.setEditable(true);
        
        grid.add(new Label("Current Quantity:"), 0, 0);
        grid.add(currentLabel, 1, 0);
        grid.add(new Label("New Quantity:"), 0, 1);
        grid.add(quantitySpinner, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });
        
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(newQuantity -> updateProductQuantity(product.getId(), newQuantity));
    }
    
    private void updateProductQuantity(String productId, int newQuantity) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("quantity", newQuantity);
        
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
                showSuccess("Quantity updated successfully");
            } else {
                showError("Failed to update quantity", response.getError());
            }
        });
        
        new Thread(task).start();
    }
    
    @FXML
    private void handleRefresh() {
        loadProducts();
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
