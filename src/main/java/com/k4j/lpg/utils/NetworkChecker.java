package com.k4j.lpg.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class NetworkChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkChecker.class);
    
    public static boolean isOnline() {
        HttpURLConnection connection = null;
        try {
            // Try to connect to Supabase
            URL url = URI.create(Config.API_BASE_URL + "/health").toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 500;
            
        } catch (Exception e) {
            logger.warn("Network check failed, assuming offline: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public static boolean isSupabaseReachable() {
        HttpURLConnection connection = null;
        try {
            String baseUrl = Config.API_BASE_URL.replace("/functions/v1/make-server-9f945771", "");
            URL url = URI.create(baseUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
            
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
