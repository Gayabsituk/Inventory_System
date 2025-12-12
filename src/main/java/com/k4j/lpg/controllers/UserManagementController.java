package com.k4j.lpg.controllers;

import com.k4j.lpg.models.User;
import com.k4j.lpg.services.ApiService;
import com.k4j.lpg.services.LocalDbService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public class UserManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    @FXML private Button addUserButton;
    
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        setupUsersTable();
        loadUsers();
    }
    
    private void setupUsersTable() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        // Role formatting
        roleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                
                if (empty || role == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(role.toUpperCase());
                    if ("admin".equalsIgnoreCase(role)) {
                        setStyle("-fx-text-fill: #292458; -fx-font-weight: bold;");
                    } else if ("staff".equalsIgnoreCase(role)) {
                        setStyle("-fx-text-fill: #065f46; -fx-font-weight: bold;");
                    }
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
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });
                
                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
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
        
        usersTable.setItems(usersList);
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
                logger.info("Users loaded: " + usersList.size());
            } else {
                showError("Failed to load users", response.getError());
            }
        });
        
        new Thread(task).start();
    }
    
    @FXML
    private void handleAddUser() {
        Dialog<java.util.Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add User");
        dialog.setHeaderText("Add New User");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("admin", "staff"));
        roleCombo.setValue("staff");
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleCombo, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
                    showError("Invalid Input", "Username and password cannot be empty");
                    return null;
                }
                
                java.util.Map<String, String> result = new java.util.HashMap<>();
                result.put("username", usernameField.getText());
                result.put("password", passwordField.getText());
                result.put("role", roleCombo.getValue());
                return result;
            }
            return null;
        });
        
        Optional<java.util.Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(this::addUser);
    }
    
    private void addUser(java.util.Map<String, String> userData) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                LocalDbService.addUser(userData.get("username"), userData.get("password"), userData.get("role"));
                return null;
            }
        };
        
        task.setOnSucceeded(event -> {
            loadUsers();
            showSuccess("User added successfully");
            logger.info("User added: " + userData.get("username"));
        });
        
        task.setOnFailed(event -> {
            showError("Failed to add user", task.getException().getMessage());
            logger.error("Failed to add user", task.getException());
        });
        
        new Thread(task).start();
    }
    
    private void handleEditUser(User user) {
        Dialog<java.util.Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit: " + user.getUsername());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField(user.getUsername());
        usernameField.setDisable(true);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Leave empty to keep current password");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("admin", "staff"));
        roleCombo.setValue(user.getRole());
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleCombo, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                java.util.Map<String, String> result = new java.util.HashMap<>();
                result.put("username", user.getUsername());
                result.put("password", passwordField.getText());
                result.put("role", roleCombo.getValue());
                return result;
            }
            return null;
        });
        
        Optional<java.util.Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(userData -> updateUser(user.getId(), userData));
    }
    
    private void updateUser(String userId, java.util.Map<String, String> userData) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String newPassword = userData.get("password");
                if (!newPassword.isEmpty()) {
                    // Update password if provided
                    LocalDbService.updateUserPassword(userId, newPassword);
                }
                LocalDbService.updateUserRole(userId, userData.get("role"));
                return null;
            }
        };
        
        task.setOnSucceeded(event -> {
            loadUsers();
            showSuccess("User updated successfully");
            logger.info("User updated: " + userData.get("username"));
        });
        
        task.setOnFailed(event -> {
            showError("Failed to update user", task.getException().getMessage());
            logger.error("Failed to update user", task.getException());
        });
        
        new Thread(task).start();
    }
    
    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete: " + user.getUsername());
        alert.setContentText("Are you sure you want to delete this user?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteUser(user);
        }
    }
    
    private void deleteUser(User user) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                LocalDbService.deleteUser(user.getId());
                return null;
            }
        };
        
        task.setOnSucceeded(event -> {
            loadUsers();
            showSuccess("User deleted successfully");
            logger.info("User deleted: " + user.getUsername());
        });
        
        task.setOnFailed(event -> {
            showError("Failed to delete user", task.getException().getMessage());
            logger.error("Failed to delete user", task.getException());
        });
        
        new Thread(task).start();
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
