package com.k4j.lpg.utils;

/**
 * Configuration constants
 * Equivalent to utils/supabase/info.tsx
 */
public class Config {
    
    // IMPORTANT: Set your Supabase credentials here if you plan to use the
    // remote API. Leave these as placeholders if you only use the local
    // SQLite-based functionality. Do NOT commit real secrets to source
    // control. Example values can be stored in an environment variable
    // or external config for production.
    public static final String SUPABASE_PROJECT_ID = "your-project-id";
    public static final String SUPABASE_ANON_KEY = "your-anon-key";
    
    // API Base URL
    public static final String API_BASE_URL = String.format(
        "https://%s.supabase.co/functions/v1/make-server-9f945771",
        SUPABASE_PROJECT_ID
    );
    
    // Low stock threshold
    public static final int LOW_STOCK_THRESHOLD = 20;
    
    // Application info
    public static final String APP_NAME = "K4J LPG Center";
    public static final String APP_VERSION = "1.0.0";
    
    // Session file location
    public static final String SESSION_FILE = System.getProperty("user.home") + "/.k4j_lpg/session.dat";
    
    private Config() {
        // Private constructor to prevent instantiation
    }
}
