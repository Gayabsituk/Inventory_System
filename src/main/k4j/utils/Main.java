package com.k4j.lpg;

import com.k4j.lpg.controllers.LoginController;
import com.k4j.lpg.utils.Config;
import com.k4j.lpg.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX Application Entry Point
 * Equivalent to App.tsx in React
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("K4J LPG Center - Inventory Management System");
        
        // Set minimum window size
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        
        try {
            // Check if user has existing session
            if (SessionManager.getInstance().hasValidSession()) {
                logger.info("Valid session found, loading dashboard");
                loadDashboard();
            } else {
                logger.info("No valid session, showing login");
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
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(Main.class.getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            
            logger.info("Login screen loaded");
            
        } catch (Exception e) {
            logger.error("Failed to load login screen", e);
            e.printStackTrace();
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
    
    public static void main(String[] args) {
        launch(args);
    }
}
