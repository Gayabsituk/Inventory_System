package com.k4j.lpg.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkChecker.class);
    
    public static boolean isOnline() {
        try {
            // Try to connect to Supabase
            URL url = new URL(Config.API_BASE_URL + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode >= 200 && responseCode < 500;
            
        } catch (Exception e) {
            logger.warn("Network check failed, assuming offline: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean isSupabaseReachable() {
        try {
            URL url = new URL(Config.API_BASE_URL.replace("/functions/v1/make-server-9f945771", ""));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode == 200;
            
        } catch (Exception e) {
            return false;
        }
    }
}