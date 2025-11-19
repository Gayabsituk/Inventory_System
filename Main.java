package com.k4j.lpg;

import com.k4j.lpg.controllers.LoginController;
import com.k4j.lpg.utils.Config;
import com.k4j.lpg.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.control.Alert;
import com.k4j.lpg.services.LocalDbService;

import org.slf4j.Logger;
import java.net.URL;
import org.slf4j.LoggerFactory;


public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Stage primaryStage;
    
    @Override
public void start(Stage stage) {
    primaryStage = stage;
    primaryStage.setTitle("K4J LPG Center - Inventory Management System");
    
    try {
        // Initialize local database
        LocalDbService.initialize();
        logger.info("Local database initialized");
        
        // Rest of startup code...
        if (SessionManager.getInstance().hasValidSession()) {
            loadDashboard();
        } else {
            showLogin();
        }
        
        primaryStage.show();
        
    } catch (Exception e) {
        logger.error("Failed to start application", e);
        e.printStackTrace();
    }
}
    
    /**
     * Show the login screen
     */
    public static void showLogin() {
        try {
            URL loginResource = Main.class.getResource("/fxml/login.fxml");
            if (loginResource == null) {
                logger.error("Could not find login.fxml resource");
                throw new RuntimeException("Login FXML file not found");
            }
            logger.info("Loading login FXML from: " + loginResource);
            
            FXMLLoader loader = new FXMLLoader(loginResource);
            Parent root = loader.load();
            
            URL cssResource = Main.class.getResource("/css/styles.css");
            if (cssResource == null) {
                logger.error("Could not find styles.css resource");
                throw new RuntimeException("CSS file not found");
            }
            logger.info("Loading CSS from: " + cssResource);
            
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(cssResource.toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            
            logger.info("Login screen loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load login screen", e);
            e.printStackTrace();
            // Show error dialog to user
            showErrorDialog("Application Error", "Failed to load login screen", e.getMessage());
        }
    }
    
    /**
     * Load appropriate dashboard based on user role
     */
    public static void loadDashboard() {
        String role = SessionManager.getInstance().getCurrentUserRole();
        
        if (role == null) {
            showLogin();
            return;
        }
        
        try {
            String fxmlFile = role.equals("admin") ? "/fxml/admin_dashboard.fxml" : "/fxml/staff_dashboard.fxml";
            
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlFile));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(Main.class.getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            
            logger.info("Dashboard loaded for role: " + role);
            
        } catch (Exception e) {
            logger.error("Failed to load dashboard", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Handle logout - clear session and show login
     */
    public static void logout() {
        SessionManager.getInstance().clearSession();
        showLogin();
        logger.info("User logged out");
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    private static void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
