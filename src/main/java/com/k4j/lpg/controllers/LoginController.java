package com.k4j.lpg.controllers;

import com.k4j.lpg.Main;
import com.k4j.lpg.models.User;
import com.k4j.lpg.services.ApiService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoginController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    @FXML
    private ImageView logoImageView;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    public void initialize() {
        // Load logo - fix for resource loading
        try {
            var logoStream = getClass().getResourceAsStream("/images/Brent-Gaz-LOGO-copy.png");
            if (logoStream == null) {
                logoStream = getClass().getResourceAsStream("/images/logo.png");
            }

            if (logoStream != null) {
                Image logo = new Image(logoStream);
                if (!logo.isError()) {
                    logoImageView.setImage(logo);
                    logoImageView.setVisible(true);
                } else {
                    logger.warn("Logo image has error: " + logo.getException());
                    logoImageView.setVisible(false);
                }
            } else {
                logger.warn("Logo image not found in resources");
                logoImageView.setVisible(false);
            }
        } catch (Exception e) {
            logger.warn("Could not load logo image", e);
            logoImageView.setVisible(false);
        }
        
        // Hide error label and progress indicator initially
        errorLabel.setVisible(false);
        progressIndicator.setVisible(false);
        
        // Add enter key handler for password field
        passwordField.setOnAction(event -> handleLogin());
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        // Disable form
        setFormDisabled(true);
        errorLabel.setVisible(false);
        progressIndicator.setVisible(true);
        
        // Perform login in background thread
        Task<ApiService.ApiResponse<User>> loginTask = new Task<>() {
            @Override
            protected ApiService.ApiResponse<User> call() {
                return ApiService.signIn(username, password);
            }
        };
        
        loginTask.setOnSucceeded(event -> {
            ApiService.ApiResponse<User> response = loginTask.getValue();
            
            if (response.isSuccess()) {
                logger.info("Login successful for: " + username);
                // Load dashboard
                Main.loadDashboard();
            } else {
                // Check if database needs initialization
                if (response.getError().contains("Invalid username") || 
                    response.getError().contains("not found")) {
                    handleDatabaseInitialization();
                } else {
                    showError(response.getError());
                    passwordField.clear();
                    setFormDisabled(false);
                }
            }
            
            progressIndicator.setVisible(false);
        });
        
        loginTask.setOnFailed(event -> {
            logger.error("Login task failed", loginTask.getException());
            showError("Login failed: " + loginTask.getException().getMessage());
            passwordField.clear();
            setFormDisabled(false);
            progressIndicator.setVisible(false);
        });
        
        new Thread(loginTask).start();
    }
    
    private void handleDatabaseInitialization() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Initialize Database");
        alert.setHeaderText("Database Not Initialized");
        alert.setContentText("The database appears to be empty. Would you like to initialize it with default data?\n\n" +
                            "Default Admin Account:\nUsername: admin\nPassword: admin123");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                initializeDatabase();
            } else {
                setFormDisabled(false);
            }
        });
    }
    
    private void initializeDatabase() {
        Task<ApiService.ApiResponse<Void>> initTask = new Task<>() {
            @Override
            protected ApiService.ApiResponse<Void> call() {
                return ApiService.initializeDatabase();
            }
        };
        
        initTask.setOnSucceeded(event -> {
            ApiService.ApiResponse<Void> response = initTask.getValue();
            
            if (response.isSuccess()) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText("Database Initialized");
                successAlert.setContentText("The database has been initialized successfully!\n\n" +
                                           "Please log in with:\nUsername: admin\nPassword: admin123");
                successAlert.showAndWait();
                
                usernameField.setText("admin");
                passwordField.clear();
            } else {
                showError("Database initialization failed: " + response.getError());
            }
            
            setFormDisabled(false);
            progressIndicator.setVisible(false);
        });
        
        initTask.setOnFailed(event -> {
            logger.error("Database initialization failed", initTask.getException());
            showError("Database initialization failed");
            setFormDisabled(false);
            progressIndicator.setVisible(false);
        });
        
        new Thread(initTask).start();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void setFormDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }
}
