package com.k4j.lpg.utils;

import com.k4j.lpg.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.prefs.Preferences;

/**
 * Manages user session and access token
 * Equivalent to localStorage and session management in api.ts
 */
public class SessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final SessionManager instance = new SessionManager();
    
    private static final String PREF_ACCESS_TOKEN = "access_token";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_USER_ROLE = "user_role";
    
    private final Preferences prefs;
    
    private String accessToken;
    private User currentUser;
    
    private SessionManager() {
        prefs = Preferences.userNodeForPackage(SessionManager.class);
        loadSession();
    }
    
    public static SessionManager getInstance() {
        return instance;
    }
    
    /**
     * Save session to persistent storage
     */
    public void saveSession(String accessToken, User user) {
        this.accessToken = accessToken;
        this.currentUser = user;
        
        prefs.put(PREF_ACCESS_TOKEN, accessToken);
        prefs.put(PREF_USER_ID, user.getId());
        prefs.put(PREF_USERNAME, user.getUsername());
        prefs.put(PREF_USER_ROLE, user.getRole());
        
        logger.info("Session saved for user: " + user.getUsername());
    }
    
    /**
     * Load session from persistent storage
     */
    private void loadSession() {
        accessToken = prefs.get(PREF_ACCESS_TOKEN, null);
        
        if (accessToken != null) {
            String userId = prefs.get(PREF_USER_ID, null);
            String username = prefs.get(PREF_USERNAME, null);
            String role = prefs.get(PREF_USER_ROLE, null);
            
            if (userId != null && username != null && role != null) {
                currentUser = new User(userId, username, role);
                logger.info("Session loaded for user: " + username);
            }
        }
    }
    
    /**
     * Clear session
     */
    public void clearSession() {
        accessToken = null;
        currentUser = null;
        
        prefs.remove(PREF_ACCESS_TOKEN);
        prefs.remove(PREF_USER_ID);
        prefs.remove(PREF_USERNAME);
        prefs.remove(PREF_USER_ROLE);
        
        logger.info("Session cleared");
    }
    
    /**
     * Check if session is valid
     */
    public boolean hasValidSession() {
        return accessToken != null && currentUser != null;
    }
    
    // Getters
    public String getAccessToken() {
        return accessToken;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }
    
    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }
    
    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
}
