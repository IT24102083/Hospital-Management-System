package com.hospital.hospitalmanagementsystem.config;

/**
 * AppConfig is a classic Singleton pattern implementation.
 * It holds application-wide configuration settings.
 * Main Components:
 * 1. A private static final instance of itself.
 * 2. A private constructor to prevent outside instantiation.
 * 3. A public static method to provide access to the single instance.
 */
public class AppConfig {

    // 1. The single, private static instance
    private static final AppConfig INSTANCE = new AppConfig();

    private final String appVersion = "2.0.0";
    private final String environment = "Production";

    // 2. Private constructor
    private AppConfig() {}

    // 3. Public static getter for the instance
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    // --- Getters for configuration properties ---
    public String getAppVersion() {
        return appVersion;
    }

    public String getEnvironment() {
        return environment;
    }
}